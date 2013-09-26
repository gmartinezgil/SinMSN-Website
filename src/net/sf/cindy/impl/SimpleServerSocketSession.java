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

import java.net.SocketAddress;
import java.util.Collection;
import java.util.Iterator;

import net.sf.cindy.SessionListener;
import net.sf.cindy.util.CopyOnWriteCollection;

/**
 * Simple server socket session. All connected socket session will have 
 * same behavior. If you want socket session connected from different
 * address will have different behavior, you should extend from 
 * ServerSocketSession and override buildSession method.
 * <p>
 * If you have set SSLContext and current vm support ssl connection, 
 * the connected socket session will be 
 * {@link net.sf.cindy.impl.SecureSocketSession secureSocketSession},
 * else it will be {@link net.sf.cindy.impl.SocketSession SocketSession}.
 * <p>
 * The connected socket sesion will use the 
 * {@link net.sf.cindy.MessageRecognizer MessageRecognizer}/
 * {@link net.sf.cindy.Dispatcher Dispatcher}/
 * {@link net.sf.cindy.EventGenerator EventGenerator}/
 *  which associted 
 * with the server socket session as it's MessageRecognizer/Dispatcher/
 * EventGenerator.
 * 
 * @author Roger Chen
 */
public class SimpleServerSocketSession extends ServerSocketSession {

    private final Collection listeners = new CopyOnWriteCollection();

    /**
     * Added socket session listener. The listener will be added to the
     * later connected socket session. 
     * 
     * @param listener
     * 		session listener
     */
    public void addSocketSessionListener(SessionListener listener) {
        if (listener != null)
            listeners.add(listener);
    }

    /**
     * Remove socket session listener. The listener won't removed from
     * connected sessions.
     * 
     * @param listener
     * 		session listener
     */
    public void removeSocketSessionListener(SessionListener listener) {
        if (listener != null)
            listeners.remove(listener);
    }

    protected SocketSession buildSession(SocketAddress address) {
        SocketSession session = null;
        if (getSSLContext() != null && Constants.SUPPORT_SSL)
            session = new SecureSocketSession();
        else
            session = new SocketSession();
        session.setMessageRecognizer(getMessageRecognizer());
        session.setDispatcher(getDispatcher());
        session.setEventGenerator(getEventGenerator());
        session.setLogException(isLogException());
        for (Iterator iter = listeners.iterator(); iter.hasNext();) {
            SessionListener listener = (SessionListener) iter.next();
            session.addSessionListener(listener);
        }
        return session;
    }

}