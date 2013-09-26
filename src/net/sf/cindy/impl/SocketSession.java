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
import java.io.Serializable;
import java.net.ConnectException;
import java.net.SocketAddress;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

/**
 * For TCP transfer. Like Socket.
 * 
 * @author Roger Chen
 */
public class SocketSession extends StreamChannelSession implements Serializable {
	private static final long serialVersionUID = 1L;
	
	private SocketAddress socketAddress;
    private SocketChannel channel;
    private SelectionKey selectionKey;

    /**
     * Set the socket address which the session will connected to.
     * 
     * @param address
     * 		the scoket address
     * @throws IllegalStateException
     */
    public void setSocketAddress(SocketAddress address)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set socket address after session started");
        this.socketAddress = address;
    }

    /**
     * Set the socket channel which the session will used.
     * 
     * @param channel
     * 		the scoket channel
     * @throws IllegalStateException
     */
    public void setChannel(SocketChannel channel) throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set socket channel after session started");
        this.channel = channel;
    }

    /**
     * Get the socket channel which the session will connected to.
     * 
     * @return
     * 		the scoket channel
     */
    public SocketChannel getChannel() {
        return channel;
    }

    public synchronized void start(boolean block) throws IllegalStateException {
        if (isStarted())
            return;
        if (socketAddress == null && channel == null)
            throw new IllegalStateException(
                    "must specify socket address or socket channel before start");
        if (channel == null) {
            try {
                channel = SocketChannel.open();
                channel.configureBlocking(false);
                channel.connect(socketAddress);
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

    /**
     * the socket is connected.
     * 
     * @return
     * 		is connected
     */
    public boolean isConnected() {
        if (channel == null) {
            return false;
        }
        return channel.isConnected();
    }

    public void onEvent(Object event, Object attachment) {
        if (event == Constants.EV_CONNECTABLE)
            onConnectable();
        super.onEvent(event, attachment);
    }

    protected void onConnectable() {
        try {
            channel.finishConnect();
            selectionKey
                    .interestOps((selectionKey.interestOps() & ~SelectionKey.OP_CONNECT)
                            | SelectionKey.OP_READ);
            dispatchSessionEstablished();
        } catch (ConnectException ce) {
            close();
        } catch (IOException e) {
            dispatchException(e);
            close();
        }
    }

    protected void onRegister(Selector selector) {
        try {
            if (isConnected()) {
                selectionKey = channel.register(selector, SelectionKey.OP_READ,
                        this);
                super.onRegister(selector);
                dispatchSessionEstablished();
            } else {
                selectionKey = channel.register(selector,
                        SelectionKey.OP_CONNECT, this);
                super.onRegister(selector);
            }
        } catch (ClosedChannelException e) {
            close();
        }
    }

    protected void onUnregister() {
        channel = null;
        selectionKey = null;
        super.onUnregister();
    }

}