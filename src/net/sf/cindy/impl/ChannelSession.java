/*
 * Copyright 2004-2005 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.sf.cindy.impl;

import java.io.IOException;
import java.nio.channels.CancelledKeyException;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cindy.Message;
import net.sf.cindy.spi.EventGeneratorSpi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * A session based on java nio Channel.
 * 
 * @author Roger Chen
 */
public abstract class ChannelSession extends AbstractTimeoutSession {

    private static final Log log = LogFactory.getLog(ChannelSession.class);

    /**
     * When use block write, this object will save all write locks.
     */
    private final Set writeLocks = Collections.synchronizedSet(new HashSet());

    private final Object closeLock = new Object();
    private final Object startLock = new Object();

    private SelectableChannel readChannel, writeChannel;
    private SelectionKey readKey, writeKey; //save this object to increase efficiency

    private volatile boolean started = false;
    private volatile boolean closing = false;

    public final boolean isStarted() {
        return started;
    }

    public final boolean isClosing() {
        return closing;
    }

    public void close(boolean block) {
        synchronized (closeLock) {
            if (!isAvailable())
                return;
            closing = true;

            EventGeneratorSpi generator = ((EventGeneratorSpi) getEventGenerator());
            generator.register(this, Constants.EV_UNREGISTER);
            if (block && (!generator.isEventGeneratorThread())) {
                try {
                    closeLock.wait();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    protected void dispatchSessionEstablished() {
        super.dispatchSessionEstablished();
        synchronized (startLock) { //When block start, will wake up
            startLock.notify();
        }
    }

    protected void startSession(SelectableChannel readChannel,
            SelectableChannel writeChannel, boolean block) {
        synchronized (startLock) {
            if (isStarted())
                return;
            try {
                if (readChannel != null)
                    readChannel.configureBlocking(false);
                if (writeChannel != null)
                    writeChannel.configureBlocking(false);
            } catch (IOException e) {
                dispatchException(e);
                if (readChannel != null)
                    try {
                        readChannel.close();
                    } catch (IOException ioe) {
                    }
                if (writeChannel != null) {
                    try {
                        writeChannel.close();
                    } catch (IOException ioe) {
                    }
                }
                dispatchSessionClosed();
                return;
            }

            this.readChannel = readChannel;
            this.writeChannel = writeChannel;
            started = true; //Set started
            EventGeneratorSpi generator = (EventGeneratorSpi) getEventGenerator();
            if (!generator.isStarted())
                generator.start();
            generator.register(this, Constants.EV_REGISTER); //If can't started, will close the session, then the started status become false
            if (block && !generator.isEventGeneratorThread())
                try {
                    startLock.wait();
                } catch (InterruptedException e) {
                }
        }
    }

    public void onEvent(Object event, Object attachment) {
        if (event == Constants.EV_READABLE)
            onReadable();
        else if (event == Constants.EV_ENABLE_READ)
            onEnableRead();
        else if (event == Constants.EV_WRITABLE)
            onWritable();
        else if (event == Constants.EV_ENABLE_WRITE)
            onEnableWrite();
        else if (event == Constants.EV_UNREGISTER)
            onUnregister();
        else if (event == Constants.EV_REGISTER)
            onRegister((Selector) attachment);
        super.onEvent(event, attachment);
    }

    private void onEnableRead() {
        if (readKey != null && readKey.isValid()) {
            readKey.interestOps(readKey.interestOps() | SelectionKey.OP_READ);
        }
    }

    private void onEnableWrite() {
        if (writeKey != null && writeKey.isValid()) {
            writeKey
                    .interestOps(writeKey.interestOps() | SelectionKey.OP_WRITE);
        }
    }

    protected void onRegister(Selector selector) {
        readKey = readChannel.keyFor(selector);
        writeKey = writeChannel.keyFor(selector);
    }

    protected void onUnregister() {
        try {
            if (readKey != null) {
                readKey.cancel();
                readKey = null;
            }
            if (readChannel != null) {
                readChannel.close();
                readChannel = null;
            }
            if (writeKey != null) {
                writeKey.cancel();
                writeKey = null;
            }
            if (writeChannel != null) {
                writeChannel.close();
                writeChannel = null;
            }
        } catch (IOException e) {
            dispatchException(e);
        }
        writeQueue.clear();

        synchronized (startLock) { //When block start, will wake up
            synchronized (closeLock) { //When block close, will wake up
                synchronized (writeLocks) {
                    for (Iterator iter = writeLocks.iterator(); iter.hasNext();) {
                        Object writeLock = (Object) iter.next();
                        iter.remove();
                        synchronized (writeLock) {
                            writeLock.notify(); //When block write, will wake up
                        }
                    }
                }
                closing = false;
                started = false;
                startLock.notify();
                closeLock.notify();
            }
        }
        dispatchSessionClosed();
    }

    private void onReadable() {
        try {
            readKey.interestOps(readKey.interestOps() & ~SelectionKey.OP_READ);
            readFromChannel(readChannel);
        } catch (CancelledKeyException cke) {
            close();
        } catch (ClosedChannelException cce) {
            close();
        } catch (IOException ioe) {
            dispatchException(ioe);
            close();
        } catch (Exception e) {
            dispatchException(e);
        }
    }

    private void onWritable() {
        try {
            writeKey.interestOps(writeKey.interestOps()
                    & ~SelectionKey.OP_WRITE);

            boolean writeComplete = false;
            while (true) {
                Object[] objs = (Object[]) writeQueue.peek();
                if (objs == null) {
                    writeComplete = true;
                    break;
                }
                Message message = writeToChannel(writeChannel, objs[0]);
                if (message != null) { //write complete
                    writeQueue.pop();
                    dispatchMessageSent(message);
                    WriteLock writeLock = (WriteLock) objs[1];
                    if (writeLock != null) {
                        writeLock.setSuccess(true);
                        synchronized (writeLock) {
                            writeLock.notify();
                        }
                    }
                } else { //not write complete, but write buffer is full
                    break;
                }
            }

            if (writeComplete)
                dispatchSessionIdle();
            else
                ((EventGeneratorSpi) getEventGenerator()).register(this,
                        Constants.EV_ENABLE_WRITE); //not write complete£¬ keep listening OP_WRITE
        } catch (CancelledKeyException cke) {
            close();
        } catch (ClosedChannelException cce) {
            close();
        } catch (IOException ioe) {
            dispatchException(ioe);
            close();
        } catch (Exception e) {
            dispatchException(e);
        }
    }

    /**
     * Read message from Channel.
     * 
     * @param channel
     * 		The channel that register OP_READ on EventGenerator
     * @throws IOException
     */
    protected void readFromChannel(SelectableChannel channel)
            throws IOException {
    }

    /**
     * Write message to Channel.
     * 
     * @param channel
     * 		The channel that register OP_WRITE on EventGenerator
     * @param writeMessage
     * 		The message peek from writeQueue
     * @return 
     * 		if not write complete, return null, else return the writen message
     * @throws IOException
     */
    protected Message writeToChannel(SelectableChannel channel,
            Object writeMessage) throws IOException {
        return null;
    }

    private void writeToWriteQueue(Object obj) {
        synchronized (writeQueue) {
            if (writeQueue.isEmpty()) {
                writeQueue.push(obj);
                ((EventGeneratorSpi) getEventGenerator()).register(this,
                        Constants.EV_ENABLE_WRITE);
            } else {
                writeQueue.push(obj);
            }
        }
    }

    /**
     * Transform the message to the object which will be write to 
     * the writeQueue.
     * 
     * @param message
     * 		the write message
     * @return
     * 		the object write to writeQueue 
     * @throws IllegalArgumentException
     */
    protected Object transMessage(Message message)
            throws IllegalArgumentException {
        return message;
    }

    public void write(Message message) throws IllegalArgumentException,
            IllegalStateException {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (!isAvailable())
            throw new IllegalStateException("session is not available");
        if (log.isTraceEnabled())
            log.trace("session " + getId() + " write message " + message);
        writeToWriteQueue(new Object[] { transMessage(message), null });
    }

    public boolean blockWrite(Message message) throws IllegalArgumentException,
            IllegalStateException {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (!isAvailable())
            throw new IllegalStateException("session is not available");
        if (log.isTraceEnabled())
            log.trace("session " + getId() + " block write message " + message);
        Object obj = transMessage(message);
        if (obj == null)
            return false;
        final WriteLock writeLock = new WriteLock();
        synchronized (writeLock) { //don't lock on message
            writeToWriteQueue(new Object[] { obj, writeLock });
            if (!((SimpleEventGenerator) getEventGenerator())
                    .isEventGeneratorThread()) {
                writeLocks.add(writeLock); //When session closed, will receive notify
                try {
                    writeLock.wait();
                } catch (InterruptedException e) {
                }
                writeLocks.remove(writeLock);
                return writeLock.isSuccess();
            } else {
                try {
                    while (true) {
                        // block write
                        Object[] objs = (Object[]) writeQueue.peek();
                        Message msg = writeToChannel(writeChannel, objs[0]);
                        if (msg != null) {
                            writeQueue.pop();
                            dispatchMessageSent(msg);
                            if (objs[1] != null) {
                                WriteLock lock = (WriteLock) objs[1];
                                lock.setSuccess(true);
                                synchronized (lock) {
                                    lock.notify();
                                }
                            }
                            if (obj == objs[0]) //write completed
                                return true;
                            continue;
                        }
                        try {
                            Thread.sleep(10); //wait next time to write
                        } catch (InterruptedException e) {
                        }
                    }
                } catch (ClosedChannelException cce) {
                    close();
                } catch (IOException ioe) {
                    dispatchException(ioe);
                    close();
                } catch (Exception e) {
                    dispatchException(e);
                }
            }
        }
        return false;
    }
}