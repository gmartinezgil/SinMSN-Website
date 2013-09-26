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
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.net.SocketAddress;
import java.nio.ByteBuffer;

import net.sf.cindy.Message;
import net.sf.cindy.PacketMessage;
import net.sf.cindy.spi.SessionStatisticSpi;

/**
 * NIO not support MulticastChannel now, so use MulticastSocket to simulate.
 * 
 * @author Roger Chen
 */
public class SimulatedMulticastSession extends AbstractSimulatedSession {

    /**
     * Actual MulticastSocket timeout
     */
    private static final int MULTICAST_SOCKET_TIMEOUT = 100;

    private InetAddress multicastAddress;
    private int bindPort = -1;
    private MulticastSocket multicastSocket;

    private MulticastSocket socket; //Actual used socket

    /**
     * Set the multicast socket bind port
     * 
     * @param port
     * 		bind port
     * @throws IllegalStateException
     */
    public void setBindPort(int port) throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set multicast port after session started");
        this.bindPort = port;
    }

    /**
     * Set the multicast address which session will join.
     * 
     * @param address
     * 		multicast address
     * @throws IllegalStateException
     */
    public void setMulticastAddress(InetAddress address)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set multicast address after session started");
        this.multicastAddress = address;
    }

    /**
     * Set the multicast socket which session will used.
     * 
     * @param socket
     * 		socket
     * @throws IllegalStateException
     */
    public void setMulticastSocket(MulticastSocket socket)
            throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set multicast socket after session started");
        this.multicastSocket = socket;
    }

    /**
     * Get multicast socket associted with the session.
     * 
     * @return
     * 		multicast socket
     */
    public MulticastSocket getMulticastSocket() {
        if (socket != null)
            return socket;
        return multicastSocket;
    }

    public boolean isStarted() {
        return socket != null;
    }

    protected void doClose() {
        if (socket != null) {
            socket.close();
            socket = null;
        }
        multicastSocket = null;
    }

    protected void doStart() throws IOException {
        if (multicastSocket == null) {
            if (bindPort >= 0 && bindPort <= 0xFFFF)
                multicastSocket = new MulticastSocket(bindPort);
            else
                multicastSocket = new MulticastSocket();
            if (multicastAddress != null)
                multicastSocket.joinGroup(multicastAddress);
        }
        multicastSocket.setSoTimeout(MULTICAST_SOCKET_TIMEOUT);
        socket = multicastSocket;
    }

    protected void checkWriteToWriteQueue(Message message)
            throws IllegalArgumentException, IllegalStateException {
        super.checkWriteToWriteQueue(message);
        if (socket == null || socket.isClosed())
            throw new IllegalStateException("session is not available");
        if (!socket.isConnected()) {
            if ((message instanceof PacketMessage)
                    && (((PacketMessage) message).getSocketAddress() != null))
                return;
            throw new IllegalArgumentException(
                    "can't send message without socket address");
        }
    }

    protected Message receive() throws IOException {
        byte[] b = new byte[readBuffer.remaining()];
        DatagramPacket packet = new DatagramPacket(b, b.length);
        socket.receive(packet);
        if (getStatistic() != null)
            ((SessionStatisticSpi) getStatistic()).received(packet.getLength());
        readBuffer.put(b, 0, packet.getLength());
        readBuffer.flip();
        Message message = recognizeMessage(readBuffer);
        if (message != null && message instanceof PacketMessage) {
            ((PacketMessage) message).setSocketAddress(packet
                    .getSocketAddress());
        }
        readBuffer.clear();
        return message;
    }

    protected void send(Message message) throws IOException {
        SocketAddress address = socket.getRemoteSocketAddress();
        if (address == null)
            address = ((PacketMessage) message).getSocketAddress();
        byte[] b = writeMessageToBytes(message);
        DatagramPacket packet = new DatagramPacket(b, 0, b.length, address);
        socket.send(packet);
        if (getStatistic() != null)
            ((SessionStatisticSpi) getStatistic()).sent(packet.getLength());
    }

    private byte[] writeMessageToBytes(Message message) {
        ByteBuffer[] buffers = message.toByteBuffer();
        int remaining = 0;
        if (buffers != null)
            for (int i = 0; i < buffers.length; i++) {
                if (buffers[i] != null)
                    remaining += buffers[i].remaining();
            }
        byte[] b = new byte[remaining];
        ByteBuffer buffer = ByteBuffer.wrap(b);
        if (buffers != null)
            for (int i = 0; i < buffers.length; i++) {
                if (buffers[i] != null)
                    buffer.put(buffers[i]);
            }
        return b;
    }

}