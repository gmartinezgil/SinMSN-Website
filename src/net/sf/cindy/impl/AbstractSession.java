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

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;

import net.sf.cindy.Dispatcher;
import net.sf.cindy.EventGenerator;
import net.sf.cindy.Message;
import net.sf.cindy.MessageRecognizer;
import net.sf.cindy.SessionListener;
import net.sf.cindy.SessionStatistic;
import net.sf.cindy.spi.DispatcherSpi;
import net.sf.cindy.spi.EventGeneratorSpi;
import net.sf.cindy.spi.SessionSpi;
import net.sf.cindy.spi.SessionStatisticSpi;
import net.sf.cindy.util.ByteBufferUtils;
import net.sf.cindy.util.CopyOnWriteCollection;
import net.sf.cindy.util.queue.Queue;
import net.sf.cindy.util.queue.QueueFactory;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Abstract Session.
 * 
 * <pre>
 * 
 *                            Application
 *  
 *                      Message         Message
 *                         |               ^
 *  Session.write()        |       |       |  MessageRecognizer.recognizer()
 *  Message.toByteBuffer() v       |       |  Message.readFromBuffer()
 *                  +------+-------|-------+------+
 *                  |              |              |
 *                  |      Abstract|Session       |
 *                  |              |              |   
 *                  |  writeQueue  |  readBuffer  |   
 *                  |              |              |
 *                  +------+-------|-------+------+
 *                         |       |       ^
 *                         |       |       |
 *                         v               |
 *       
 *                              Network
 *  
 * </pre>
 * 
 * @author Roger Chen
 */
public abstract class AbstractSession implements SessionSpi {

    private static final Log log = LogFactory.getLog(AbstractSession.class);

    /**
     * Default event generator
     */
    public static final EventGenerator DEFAULT_EVENT_GENERATOR = new AutoCloseEventGenerator();

    private static int sessionId;
    private final int id = sessionId++;

    /**
     * Read cache£¬All data read from IO will put here
     */
    protected ByteBuffer readBuffer = ByteBufferUtils.allocate(
            Constants.BUFFER_CAPACITY, Constants.USE_DIRECT_BUFFER);

    /**
     * Write queue, all date write to IO will put here
     */
    protected final Queue writeQueue = QueueFactory.createQueue();

    private SessionStatisticSpi statistic = new SimpleSessionStatistic(this);
    private EventGeneratorSpi eventGenerator = (EventGeneratorSpi) DEFAULT_EVENT_GENERATOR;
    private DispatcherSpi dispatcher = new SimpleDispatcher();
    private MessageRecognizer messageRecognizer = new ByteArrayMessageRecognizer();
    private Object attachment;

    private boolean enableStatistic = false;
    private final Collection sessionListeners = new CopyOnWriteCollection();
    private int sessionTimeout = Constants.SESSION_TIMEOUT;
    private boolean logException = Constants.LOG_EXCEPTION;
    private int bufferCapacityLimit = Constants.BUFFER_CAPACITY_LIMIT;

    public final int getId() {
        return id;
    }

    public final boolean getEnableStatistic() {
        return enableStatistic;
    }

    public final void setEnableStatistic(boolean enable) {
        enableStatistic = enable;
    }

    public final SessionStatistic getStatistic() {
        if (enableStatistic)
            return statistic;
        return null;
    }

    public final void setStatistic(SessionStatistic statistic) {
        if (statistic != null) {
            if (!(statistic instanceof SessionStatisticSpi))
                throw new IllegalArgumentException(
                        "session statistic must implement SessionStatisticSpi interface");
            if (isStarted())
                throw new IllegalStateException(
                        "can't set session statistic when session have already started");
            this.statistic = (SessionStatisticSpi) statistic;
        }
    }

    public final EventGenerator getEventGenerator() {
        return eventGenerator;
    }

    public final void setEventGenerator(EventGenerator generator) {
        if (generator != null) {
            if (!(generator instanceof EventGeneratorSpi))
                throw new IllegalArgumentException(
                        "event generator must implement EventGeneratorSpi interface");
            if (isStarted())
                throw new IllegalStateException(
                        "can't set event generator when session have already started");
            this.eventGenerator = (EventGeneratorSpi) generator;
        }
    }

    public void setDispatcher(Dispatcher dispatcher) {
        if (dispatcher != null) {
            if (!(dispatcher instanceof DispatcherSpi))
                throw new IllegalArgumentException(
                        "dispatcher must implement DispatcherSpi interface");
            this.dispatcher = (DispatcherSpi) dispatcher;
        }
    }

    public Dispatcher getDispatcher() {
        return dispatcher;
    }

    public final Object getAttachment() {
        return attachment;
    }

    public final void setAttachment(Object attachment) {
        this.attachment = attachment;
    }

    public final int getSessionTimeout() {
        return sessionTimeout;
    }

    public final void setSessionTimeout(int sessionTimeout) {
        if (sessionTimeout < 0)
            sessionTimeout = 0;
        this.sessionTimeout = sessionTimeout;
    }

    public final MessageRecognizer getMessageRecognizer() {
        return messageRecognizer;
    }

    public final void setMessageRecognizer(MessageRecognizer messageRecognizer) {
        if (messageRecognizer != null)
            this.messageRecognizer = messageRecognizer;
    }

    public final boolean isLogException() {
        return logException;
    }

    public final void setLogException(boolean logException) {
        this.logException = logException;
    }

    public final int getBufferCapacityLimit() {
        return bufferCapacityLimit;
    }

    public final void setBufferCapacityLimit(int bufferCapacityLimit) {
        if (bufferCapacityLimit > readBuffer.capacity())
            this.bufferCapacityLimit = bufferCapacityLimit;
    }

    public final void addSessionListener(SessionListener listener) {
        if (listener != null)
            sessionListeners.add(listener);
    }

    public final void removeSessionListener(SessionListener listener) {
        if (listener != null)
            sessionListeners.remove(listener);
    }

    public final void start() throws IllegalStateException {
        start(false);
    }

    public final void close() {
        close(false);
    }

    public final boolean isAvailable() {
        return isStarted() && !isClosing();
    }

    public int getWriteQueueSize() {
        return writeQueue.size();
    }

    public void onEvent(Object event, Object attachment) {
        if (log.isTraceEnabled())
            log.trace("session " + getId() + " on event: [Event] " + event
                    + " [Attachment] " + attachment);
    }

    /**
     * Get all session listeners in current session.
     * 
     * @return
     * 		all session listeners
     */
    protected final Iterator getSessionListeners() {
        return sessionListeners.iterator();
    }

    private void dispatch(Runnable runnable) {
        dispatcher.dispatch(this, runnable);
    }

    public void dispatchException(final Throwable cause) {
        if (log.isDebugEnabled())
            log.debug("session " + sessionId + " caught exception " + cause);
        if (isLogException()) {
            log.error(cause, cause); //Log exception
        }
        dispatch(new DispatchObject() {

            protected void doRun() throws Exception {
                for (Iterator iter = sessionListeners.iterator(); iter
                        .hasNext();) {
                    SessionListener listener = (SessionListener) iter.next();
                    listener.exceptionCaught(AbstractSession.this, cause);
                }
            }
        });
    }

    protected void dispatchSessionEstablished() {
        if (log.isDebugEnabled())
            log.debug("session " + sessionId + " established");
        statistic.start(); //Start statistic
        dispatch(new DispatchObject() {

            protected void doRun() throws Exception {
                for (Iterator iter = sessionListeners.iterator(); iter
                        .hasNext();) {
                    SessionListener listener = (SessionListener) iter.next();
                    listener.sessionEstablished(AbstractSession.this);
                }
            }
        });
    }

    protected void dispatchSessionClosed() {
        if (log.isDebugEnabled())
            log.debug("session " + sessionId + " closed");
        statistic.stop(); //Stop statistic
        dispatch(new DispatchObject() {

            protected void doRun() throws Exception {
                for (Iterator iter = sessionListeners.iterator(); iter
                        .hasNext();) {
                    SessionListener listener = (SessionListener) iter.next();
                    listener.sessionClosed(AbstractSession.this);
                }
            }
        });
    }

    protected void dispatchSessionIdle() {
        if (log.isDebugEnabled())
            log.debug("session " + sessionId + " idle");
        dispatch(new DispatchObject() {

            protected void doRun() throws Exception {
                for (Iterator iter = sessionListeners.iterator(); iter
                        .hasNext();) {
                    SessionListener listener = (SessionListener) iter.next();
                    listener.sessionIdle(AbstractSession.this);
                }
            }
        });
    }

    protected void dispatchSessionTimeout() {
        if (log.isDebugEnabled())
            log.debug("session " + sessionId + " timeout");
        dispatch(new DispatchObject() {

            protected void doRun() throws Exception {
                for (Iterator iter = sessionListeners.iterator(); iter
                        .hasNext();) {
                    SessionListener listener = (SessionListener) iter.next();
                    listener.sessionTimeout(AbstractSession.this);
                }
            }
        });
    }

    protected void dispatchMessageReceived(final Message message) {
        if (log.isDebugEnabled())
            log.debug("session " + sessionId + " received message " + message);
        dispatch(new DispatchObject() {

            protected void doRun() throws Exception {
                for (Iterator iter = sessionListeners.iterator(); iter
                        .hasNext();) {
                    SessionListener listener = (SessionListener) iter.next();
                    listener.messageReceived(AbstractSession.this, message);
                }
            }
        });
    }

    protected void dispatchMessageSent(final Message message) {
        if (log.isDebugEnabled())
            log.debug("session " + sessionId + " sent message " + message);
        dispatch(new DispatchObject() {

            protected void doRun() throws Exception {
                for (Iterator iter = sessionListeners.iterator(); iter
                        .hasNext();) {
                    SessionListener listener = (SessionListener) iter.next();
                    listener.messageSent(AbstractSession.this, message);
                }
            }
        });
    }

    /**
     * Recognize message from ByteBuffer.
     * 
     * @param buffer
     *            The ByteBuffer contains data
     * @return Recognized message.
     */
    protected Message recognizeMessage(ByteBuffer buffer) {
        try {
            if (log.isTraceEnabled())
                log.trace("session " + sessionId + " recognize message");

            Message message = messageRecognizer.recognize(this, buffer
                    .asReadOnlyBuffer());
            if (message != null) {
                boolean completed = message.readFromBuffer(buffer);
                if (log.isTraceEnabled())
                    log.trace("session " + sessionId + " recognized message "
                            + message.getClass().getName()
                            + (completed ? " " : " not ") + "completed");
                if (completed)
                    return message;
            }
        } catch (Exception e) { //Protection catch
            dispatchException(e);
        }
        return null;
    }

    private abstract class DispatchObject implements Runnable {

        public void run() {
            try {
                doRun();
            } catch (Exception e) {
                dispatchException(e);
            }
        }

        protected abstract void doRun() throws Exception;
    }

    /**
     * This class act as a lock when block write message.
     * 
     * @author Roger Chen
     */
    protected static class WriteLock {

        private boolean success;

        public boolean isSuccess() {
            return success;
        }

        public void setSuccess(boolean success) {
            this.success = success;
        }
    }

}