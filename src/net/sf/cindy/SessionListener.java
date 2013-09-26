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
 * Session Listener.
 * 
 * @author Roger Chen
 */
public interface SessionListener {

    /**
     * Session have established.
     * 
     * @param session
     * 		session
     * @throws Exception
     */
    public void sessionEstablished(Session session) throws Exception;

    /**
     * Session have closed or refused.
     * 
     * @param session
     * 		session
     * @throws Exception
     */
    public void sessionClosed(Session session) throws Exception;

    /**
     * Session idle. For example, if you want send a large file,
     * you can listen on this event and send some pieces when session
     * idle. 
     * 
     * @param session
     * 		session
     * @throws Exception
     */
    public void sessionIdle(Session session) throws Exception;

    /**
     * Session timeout, but not closed.
     * 
     * @param session
     * 		session
     * @throws Exception
     */
    public void sessionTimeout(Session session) throws Exception;

    /**
     * Session received a message.
     * 
     * @param session 
     * 		session
     * @param message
     * 		the received message
     * @throws Exception
     */
    public void messageReceived(Session session, Message message)
            throws Exception;

    /**
     * Session sent a message.
     * 
     * @param session
     * 		session
     * @param message
     * 		the sent message
     * @throws Exception
     */
    public void messageSent(Session session, Message message) throws Exception;

    /**
     * Session caught a exception.
     * 
     * @param session
     * 		session
     * @param cause
     * 		exception
     * @throws Exception
     */
    public void exceptionCaught(Session session, Throwable cause)
            throws Exception;
}