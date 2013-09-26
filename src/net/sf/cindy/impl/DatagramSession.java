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
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ClosedChannelException;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

import net.sf.cindy.Message;
import net.sf.cindy.PacketMessage;
import net.sf.cindy.spi.EventGeneratorSpi;
import net.sf.cindy.spi.SessionStatisticSpi;
import net.sf.cindy.util.ByteBufferUtils;

/**
 * For UDP transfer, when MessageRecognizer recognized
 * a message and the message instance of PacketMessage,
 * will invoke PacketMessage.setSocketAddress method.
 * 
 * @author Roger Chen
 */
public class DatagramSession extends PacketChannelSession {

    private DatagramChannel channel;
    private SocketAddress localAddress;
    private SocketAddress remoteAddress;

    /**
     * Set the local address which session listen on.
     * 
     * @param address
     * 		local address
     * @throws IllegalStateException
     */
    public void setLocalAddress(SocketAddress address)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set local address after session started");
        this.localAddress = address;
    }

    /**
     * Set the remote address which session connected to.
     * 
     * @param address
     * 		remote address
     * @throws IllegalStateException
     */
    public void setRemoteAddress(SocketAddress address)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set remote address after session started");
        this.remoteAddress = address;
    }

    /**
     * Set the datagram channel which the session will used.
     * 
     * @param channel
     * 		datagram channel
     * @throws IllegalStateException
     */
    public void setChannel(DatagramChannel channel)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set datagram channel after session started");
        this.channel = channel;
    }

    /**
     * Get datagram channel associted with the session.
     * 
     * @return
     * 		datagram channel
     */
    public DatagramChannel getChannel() {
        return channel;
    }

    public synchronized void start(boolean block) throws IllegalStateException {
        if (isStarted())
            return;
        if (channel == null) {
            try {
                channel = DatagramChannel.open();
                channel.socket().bind(localAddress);
                if (remoteAddress != null)
                    channel.connect(remoteAddress);
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
     * the session is connected.
     * 
     * @return
     * 		is connected
     */
    public boolean isConnected() {
        if (channel == null)
            return false;
        return channel.isConnected();
    }

    protected void onRegister(Selector selector) {
        try {
            channel.register(selector, SelectionKey.OP_READ, this);
            super.onRegister(selector);
            dispatchSessionEstablished();
        } catch (ClosedChannelException e) {
            close();
        }
    }

    protected void onUnregister() {
        channel = null;
        super.onUnregister();
    }

    protected void readFromChannel(SelectableChannel channel)
            throws IOException {
        while ((lastSocketAddress = ((DatagramChannel) channel)
                .receive(readBuffer)) != null) {
            readBuffer.flip();
            if (getStatistic() != null)
                ((SessionStatisticSpi) getStatistic()).received(readBuffer
                        .remaining());
            Message message = recognizeMessage(readBuffer);
            if (message != null)
                dispatchMessageReceived(message);
            readBuffer.clear();
        }
        ((EventGeneratorSpi) getEventGenerator()).register(this,
                Constants.EV_ENABLE_READ);
    }

    protected final Object transMessage(Message message)
            throws IllegalArgumentException {
        if (!canWriteToQueue(message))
            throw new IllegalArgumentException(
                    "can't send message without socket address");
        return super.transMessage(message);
    }

    private boolean canWriteToQueue(Message message) {
        if (isConnected())
            return true;
        if (message instanceof PacketMessage)
            if (((PacketMessage) message).getSocketAddress() != null)
                return true;
        return false;
    }

    protected Message writeToChannel(SelectableChannel channel,
            Object writeMessage) throws IOException {
        Message message = (Message) writeMessage;
        ByteBuffer[] buffers = message.toByteBuffer();

        if (ByteBufferUtils.hasRemaining(buffers)) {
            long writeCount = 0;
            if (isConnected()) {
                writeCount = getChannel().write(buffers);
            } else {
                SocketAddress address = ((PacketMessage) message)
                        .getSocketAddress();
                //for reduce memory cost
                ByteBuffer writeBuffer = buffers.length == 1 ? buffers[0]
                        : ByteBufferUtils.gather(buffers);
                writeCount = getChannel().send(writeBuffer, address);
            }

            if (getStatistic() != null)
                ((SessionStatisticSpi) getStatistic()).sent(writeCount);
            if (writeCount == 0)
                return null;
        }
        return message; //Write completed
    }
}