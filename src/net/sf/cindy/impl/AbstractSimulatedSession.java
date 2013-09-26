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
import java.net.SocketTimeoutException;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import net.sf.cindy.Message;
import net.sf.cindy.util.ElapsedTime;
import net.sf.cindy.util.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * NIO not support Multicast, and in JDK 1.4, NIO not support SSL Socket.
 * Simulated session use the class in io package  to simulate non-blocking
 * action, but without non-blocking action efficiency.
 * 
 * @author Roger Chen
 */
public abstract class AbstractSimulatedSession extends AbstractSession {

    private static final Log log = LogFactory
            .getLog(AbstractSimulatedSession.class);

    private final Set writeLocks = Collections.synchronizedSet(new HashSet());

    private volatile boolean closing;

    private SimulateThread thread;

    private int idleTime;

    public boolean isClosing() {
        return closing;
    }

    public void close(boolean block) {
        if (!isAvailable())
            return;
        synchronized (this) {
            closing = true;
            synchronized (writeLocks) {
                for (Iterator iter = writeLocks.iterator(); iter.hasNext();) {
                    Object writeLock = (Object) iter.next();
                    iter.remove();
                    synchronized (writeLock) {
                        writeLock.notify(); //Notify
                    }
                }
            }
            idleTime = 0;
            doClose();
            if (Thread.currentThread() != thread && thread != null) {
                while (thread.isAlive()) {
                    try {
                        thread.join();
                    } catch (InterruptedException e) {
                    }
                }
            }
            thread = null;
            closing = false;
            dispatchSessionClosed();
        }
    }

    protected abstract void doClose();

    public synchronized void start(boolean block) throws IllegalStateException {
        if (isStarted())
            return;
        try {
            doStart();
        } catch (IOException e) {
            dispatchException(e);
            doClose();
            dispatchSessionClosed();
            return;
        }
        idleTime = 0;
        dispatchSessionEstablished();
        thread = new SimulateThread();
        thread.start();
    }

    protected abstract void doStart() throws IOException;

    public void onEvent(Object event, Object attachment) {
        if (event == Constants.EV_UNREGISTER)
            close();
        super.onEvent(event, attachment);
    }

    protected void checkWriteToWriteQueue(Message message)
            throws IllegalArgumentException, IllegalStateException {
        if (message == null)
            throw new IllegalArgumentException("message is null");
        if (!isAvailable())
            throw new IllegalStateException("session is not available");
    }

    public void write(Message message) throws IllegalArgumentException,
            IllegalStateException {
        checkWriteToWriteQueue(message);
        if (log.isTraceEnabled())
            log.trace("session " + getId() + " write message " + message);
        writeQueue.push(new Object[] { message, null });
    }

    public boolean blockWrite(Message message) throws IllegalArgumentException,
            IllegalStateException {
        checkWriteToWriteQueue(message);
        if (log.isTraceEnabled())
            log.trace("session " + getId() + " block write message " + message);
        final WriteLock writeLock = new WriteLock();
        synchronized (writeLock) {
            writeQueue.push(new Object[] { message, writeLock });
            if (Thread.currentThread() != thread) {
                writeLocks.add(writeLock);
                synchronized (writeLock) {
                    try {
                        writeLock.wait();
                    } catch (InterruptedException e) {
                    }
                }
                writeLocks.remove(writeLock);
                return writeLock.isSuccess();
            } else {
                try {
                    sendLoop(message);
                } catch (IOException e) {
                    if (isStarted()) {
                        dispatchException(e);
                        close();
                    }
                }
            }
        }
        return false;
    }

    private void sendLoop(Message blockWriteMessage) throws IOException {
        while (isStarted()) {
            Object[] objs = (Object[]) writeQueue.pop();
            if (objs == null)
                break;
            Message message = (Message) objs[0];
            WriteLock writeLock = (WriteLock) objs[1];

            send(message);
            dispatchMessageSent(message);
            idleTime = 0;
            if (writeLock != null) {
                writeLock.setSuccess(true);
                synchronized (writeLock) {
                    writeLock.notify();
                }
            }
            if (message == blockWriteMessage)
                return;
        }
    }

    protected abstract void send(Message message) throws IOException;

    protected abstract Message receive() throws IOException;

    private static int i; //Used to build thread name

    private class SimulateThread extends Thread {

        public SimulateThread() {
            super(Utils.getClassSimpleName(AbstractSimulatedSession.this
                    .getClass())
                    + "-" + i++);
        }

        public void run() {
            try {
                while (isStarted()) {
                    ElapsedTime elapsedTime = new ElapsedTime();
                    try {
                        Message message = receive();
                        if (message != null)
                            dispatchMessageReceived(message);
                    } catch (SocketTimeoutException ste) {
                        idleTime += elapsedTime.getElapsedTime();
                    }
                    sendLoop(null);
                    if (idleTime >= getSessionTimeout()) {
                        dispatchSessionTimeout();
                        idleTime = 0;
                    }
                }
            } catch (IOException e) {
                if (isStarted())
                    dispatchException(e);
            } finally {
                close();
            }
        }

    }
}