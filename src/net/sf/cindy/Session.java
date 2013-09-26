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
package net.sf.cindy;

/**
 * Session interface.
 * 
 * <pre>
 * 
 *     +----->---->-----+
 *     |                |    Event
 *     ^ EventGenerator v----------->Session.onEvent()
 *     |                |
 *     +-----<----<-----+
 * 
 * </pre>
 * <hr/>
 * <pre>
 *             ListenerEvent             ListenerEvent(may be in other thread)
 *    Session---------------->Dispatcher---------------->SessionListener 
 * 
 * </pre>
 * <hr/>
 * <pre>
 *                         Application
 *       
 *                     Message     Message
 *                        |           ^
 *     Session.write()    |     |     |   MessageRecognizer.recognizer()
 * Message.toByteBuffer() v     |     |   Message.readFromBuffer()
 *                  +-----+-----|-----+-----+
 *                  |                       |
 *                  |        Session        | 
 *                  |                       |
 *                  +-----+-----|-----+-----+
 *                        |     |     ^
 *                        |     |     |
 *                        v           |
 *      
 *                           Network
 * </pre>
 * 
 * @author Roger Chen
 */
public interface Session {

    /**
     * Get session id. The id must be unique in current class loader.
     * 
     * @return
     * 		session id
     */
    public int getId();

    /**
     * If enable statistic, getStatistic method will return a SessionStatistic,
     * otherwise will return null.
     * <p>
     * Disable statistic will improve performance slightly.
     * 
     * @param enable
     * 		enable statistic
     */
    public void setEnableStatistic(boolean enable);

    /**
     * Get statistic is enabled, if it return false, getStatistic method will
     * return null.
     * 
     * @return
     * 		statistic enabled 
     */
    public boolean getEnableStatistic();

    /**
     * Get the {@link net.sf.cindy.SessionStatistic SessionStatistic} associated 
     * with the session.
     * 
     * @return
     * 		session statistic
     */
    public SessionStatistic getStatistic();

    /**
     * Set the {@link net.sf.cindy.SessionStatistic SessionStatistic} associated 
     * with the session.
     * 
     * @param statistic
     * 		session statistic
     */
    public void setStatistic(SessionStatistic statistic);

    /**
     * Get the {@link net.sf.cindy.EventGenerator EventGenerator} associated
     * with the session, if any.
     * 
     * @return
     * 		the event generator associated with the session 
     */
    public EventGenerator getEventGenerator();

    /**
     * Set the {@link net.sf.cindy.EventGenerator EventGenerator} associated
     * with the session.
     * 
     * @param generator 
     * 		the event generator
     */
    public void setEventGenerator(EventGenerator generator);

    /**
     * Get the {@link net.sf.cindy.Dispatcher Dispatcher} associated
     * with the session, if any.
     * 
     * @return
     * 		the dispatcher associated with the session 
     */
    public Dispatcher getDispatcher();

    /**
     * Set the {@link net.sf.cindy.Dispatcher Dispatcher} associated
     * with the session.
     * 
     * @param dispatcher
     * 		dispatcher
     */
    public void setDispatcher(Dispatcher dispatcher);

    /**
     * Get session attachment.
     * 
     * @return
     * 		attachment
     */
    public Object getAttachment();

    /**
     * Set session attachment.
     * 
     * @param attachment
     * 		attachment
     */
    public void setAttachment(Object attachment);

    /**
     * Get the {@link net.sf.cindy.MessageRecognizer MessageRecognizer} associated
     * with the session.
     * 
     * @return
     * 		the <code>MessageRecognizer</code> associated with the session 
     */
    public MessageRecognizer getMessageRecognizer();

    /**
     * Set the {@link net.sf.cindy.MessageRecognizer MessageRecognizer} associated
     * with the session.
     * 
     * @param messageRecognizer 
     * 		the message recognizer
     */
    public void setMessageRecognizer(MessageRecognizer messageRecognizer);

    /**
     * Set session timeout, in millisecond.
     * 
     * @param timeout
     * 		session timeout
     */
    public void setSessionTimeout(int timeout);

    /**
     * Get session timeout, in millisecond.
     * 
     * @return
     * 		session timeout
     */
    public int getSessionTimeout();

    /**
     * If log exception, when exception happen, will log exception.
     * 
     * @return 
     * 		is log exception
     */
    public boolean isLogException();

    /**
     * If log exception, when exception happen, will log exception.
     * 
     * @param logException
     * 		is log exception
     */
    public void setLogException(boolean logException);

    /**
     * To prevent attack, if read buffer greater than this, and 
     * {@link net.sf.cindy.MessageRecognizer MessageRecognizer} can't
     * recognize the message, session will be closed.
     *
     * @return
     * 		read buffer capacity limit
     */
    public int getBufferCapacityLimit();

    /**
     * To prevent attack, if read buffer greater than this, and 
     * {@link net.sf.cindy.MessageRecognizer MessageRecognizer} can't
     * recognize the message, session will be closed. the value MUST
     * greater than current read buffer capacity.
     *
     * @param bufferCapacityLimit
     * 		max read buffer capacity
     */
    public void setBufferCapacityLimit(int bufferCapacityLimit);

    /**
     * Add session listener.
     * 
     * @param listener
     * 		session listener
     */
    public void addSessionListener(SessionListener listener);

    /**
     * Remove session listener.
     * 
     * @param listener
     * 		session listener
     */
    public void removeSessionListener(SessionListener listener);

    /**
     * Start the session. If block start the session, the method will 
     * block until session successful started or failed, call isAvailable()
     * to judge the session have successful started.
     * 
     * @param block
     * 		is block
     * @throws IllegalStateException
     */
    public void start(boolean block) throws IllegalStateException;

    /**
     * Start the session. This method is a shorthand for:
     * <blockquote><pre>
     *     start(false);
     * </pre></blockquote>
     * 
     * @throws IllegalStateException
     */
    public void start() throws IllegalStateException;

    /**
     * Close the session. When block is true, the method will return until the
     * session have been closed. If you invoke this method in the dispatch
     * thread, You can't receive events until the method return.
     * 
     * @param block
     * 		blcok until session have closed
     */
    public void close(boolean block);

    /**
     * Close the session. This method is a shorthand for:
     * <blockquote><pre>
     *     close(false);
     * </pre></blockquote>
     */
    public void close();

    /**
     * Current session is closing now.
     * 
     * @return
     * 		current session is closing now
     */
    public boolean isClosing();

    /**
     * Current session have started.
     * 
     * @return
     * 		current session have started
     */
    public boolean isStarted();

    /**
     * Have started and isn't closing now.
     * 
     * @return 
     * 		current session is available
     */
    public boolean isAvailable();

    /**
     * Get message count in current write queue.
     * 
     * @return
     * 		write queue size
     */
    public int getWriteQueueSize();

    /**
     * Write message. If the message have successfully sent, 
     * messageSent event will generate.
     * 
     * @param message
     * 		message
     * @throws IllegalArgumentException
     * 		if message is null, or need the message is
     * not {@link net.sf.cindy.PacketMessage PacketMessage} but requires 
     * PacketMessage
     * @throws IllegalStateException
     * 		if current session is not available
     */
    public void write(Message message) throws IllegalArgumentException,
            IllegalStateException;

    /**
     * Write message. The method will return after write message finished.
     * 
     * @param message
     * 		message
     * @return
     * 		write successful
     * @throws IllegalArgumentException
     * 		if message is null, or need the message is
     * not {@link net.sf.cindy.PacketMessage PacketMessage} but requires 
     * PacketMessage
     * @throws IllegalStateException
     * 		if current session is not available
     */
    public boolean blockWrite(Message message) throws IllegalArgumentException,
            IllegalStateException;

    /**
     * Dispatch exception. Let the session listener handle the exception.
     * 
     * @param throwable
     * 		throwable
     * @deprecated
     */
    public void dispatchException(Throwable throwable);

}