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
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.SSLContext;

import net.sf.cindy.Message;
import net.sf.cindy.Session;
import net.sf.cindy.SessionAdapter;
import net.sf.cindy.SessionListener;

/**
 * Like ServerSocket.
 * 
 * @author Roger Chen
 */
public abstract class ServerSocketSession extends ChannelSession {

    private SocketAddress socketAddress;
    private ServerSocketChannel channel;

    private final Set sessions = new HashSet();
    private boolean closeAllSocketSessions;
    private SSLContext sslc; //the default SSLContext for every connected secure socket

    /**
     * Manage all connected sessions.
     */
    private final SessionListener listener = new SessionAdapter() {

        public void sessionClosed(Session session) {
            session.removeSessionListener(this);
            synchronized (sessions) {
                sessions.remove(session);
            }
        }
    };

    /**
     * Set the default SSLContext for every connected secure socket.
     * 
     * @param sslc
     * 		default SSLContext
     */
    public void setSSLContext(SSLContext sslc) {
        this.sslc = sslc;
    }

    /**
     * Get the default SSLContext for every connected secure socket.
     * 
     * @return
     * 		SSLContext
     */
    public SSLContext getSSLContext() {
        return sslc;
    }

    /**
     * When close server socket session, default the connected socket 
     * session will not be closed. Set the value to ture, all connected
     * socket session will be closed after server socket session closed.
     * 
     * @param closeAllSocketSessions
     * 		close all connected socket session after server socket session
     * closed
     */
    public void setCloseAllSocketSessions(boolean closeAllSocketSessions) {
        this.closeAllSocketSessions = closeAllSocketSessions;
    }

    /**
     * When close server socket session, default the connected socket 
     * session will not be closed. If the value is ture, all connected
     * socket session will be closed after server socket session closed.
     * 
     * @return
     * 		close all connected socket session after server socket session
     * closed
     */
    public boolean isCloseAllSocketSessions() {
        return closeAllSocketSessions;
    }

    /**
     * Set the port the server socket session listen to.
     * 
     * @param port
     * 		listen port
     * @throws IllegalStateException
     */
    public void setListenPort(int port) throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set listen port after session started");
        this.socketAddress = new InetSocketAddress(port);
    }

    /**
     * Set the address the server socket sesion listen to.
     * 
     * @param address
     * 		listen address
     * @throws IllegalStateException
     */
    public void setListenAddress(SocketAddress address)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set listen address after session started");
        this.socketAddress = address;
    }

    /**
     * Set the server socket channel which the session will used.
     * 
     * @param ssc
     * 		server socket channel
     * @throws IllegalStateException
     */
    public void setChannel(ServerSocketChannel ssc)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set server socket channel after session started");
        this.channel = ssc;
    }

    /**
     * Get server socket channel associted with the session.
     * 
     * @return
     * 		server socket channel
     */
    public ServerSocketChannel getChannel() {
        return channel;
    }

    /**
     * Get connected socket sessions.
     * 
     * @return
     * 		connected socket sessions.
     */
    public SocketSession[] getConnectedSessions() {
        synchronized (sessions) {
            SocketSession[] ss = new SocketSession[sessions.size()];
            sessions.toArray(ss);
            return ss;
        }
    }

    public synchronized void start(boolean block) throws IllegalStateException {
        if (isStarted())
            return;
        if (channel == null) {
            try {
                channel = ServerSocketChannel.open();
                channel.socket().bind(
                        socketAddress == null ? new InetSocketAddress(0)
                                : socketAddress);
            } catch (IOException e) {
                dispatchException(e);
                if (channel != null) {
                    try {
                        channel.close();
                    } catch (IOException e1) {
                    }
                    channel = null;
                }
                dispatchSessionClosed();
                return;
            }
        }
        startSession(channel, channel, block);
    }

    public void onEvent(Object event, Object attachment) {
        if (event == Constants.EV_ACCEPTABLE)
            onAcceptable();
        super.onEvent(event, attachment);
    }

    protected void onRegister(Selector selector) {
        try {
            channel.register(selector, SelectionKey.OP_ACCEPT, this);
            super.onRegister(selector);
            dispatchSessionEstablished();
        } catch (ClosedChannelException e) {
            close();
        }
    }

    protected void onUnregister() {
        if (closeAllSocketSessions) {
            SocketSession[] ss = getConnectedSessions();
            for (int i = 0; i < ss.length; i++) {
                ss[i].close();
            }
        }
        synchronized (sessions) {
            sessions.clear();
        }
        channel = null;
        super.onUnregister();
    }

    protected void onAcceptable() {
        try {
            SocketChannel sc = getChannel().accept();
            SocketSession session = buildSession(sc.socket()
                    .getRemoteSocketAddress());
            if (session != null) {
                //SSL
                if (session instanceof SecureSocketSession) {
                    SecureSocketSession sss = (SecureSocketSession) session;
                    initSecureSocketSession(sss);
                }
                session.setChannel(sc);
                session.addSessionListener(listener);
                synchronized (sessions) {
                    sessions.add(session);
                }
                session.start();
            } else {
                sc.socket().setSoLinger(true, 0); //Won't be TIME_WAIT status
                sc.close();
            }
        } catch (IOException e) {
            dispatchException(e);
        }
    }

    protected void initSecureSocketSession(SecureSocketSession sss) {
        if (sslc != null)
            sss.setSSLContext(sslc);
        sss.setUseClientMode(false);
    }

    public void write(Message message) {
        throw new UnsupportedOperationException(
                "can't write message to ServerSocketSession");
    }

    public boolean blockWrite(Message message) {
        throw new UnsupportedOperationException(
                "can't write message to ServerSocketSession");
    }

    /**
     * Build accepted session. Application can choose to return SocketSession 
     * or SecureSocketSession.
     * 
     * @param address
     * 		the remote socket address that ServerSocketChannel accepted
     * @return
     * 		If return null, the connected socket will be closed. Otherwise 
     * will start the returned SocketSession.		
     */
    protected abstract SocketSession buildSession(SocketAddress address);

}