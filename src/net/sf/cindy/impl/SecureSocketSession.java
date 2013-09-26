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
import java.net.Socket;
import java.nio.ByteBuffer;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;
import javax.net.ssl.SSLEngineResult;
import javax.net.ssl.SSLException;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLEngineResult.HandshakeStatus;
import javax.net.ssl.SSLEngineResult.Status;

import net.sf.cindy.Message;
import net.sf.cindy.util.ByteBufferUtils;
import net.sf.cindy.util.queue.Queue;
import net.sf.cindy.util.queue.QueueFactory;

/**
 * For Secure TCP Socket Transfer, only support JDK 1.5.
 * <p>
 * <pre>
 *                   Application Data
 * 
 *               Message      appReadBuffer
 *                  |               ^
 *                  |       |       |
 *                  v       |       |
 *           +------+-------|-------+------+
 *           |              |              |
 *           |           SSL|Engine        |
 *   wrap()  |              |              |  unwrap()
 *           |   OUTBOUND   |   INBOUND    |
 *           |              |              |
 *           +------+-------|-------+------+
 *                  |       |       ^
 *                  |       |       |
 *                  v               |
 *            netWriteBuffer    readBuffer
 * 
 *                      Net data
 * </pre>
 * 
 * @author Roger Chen
 */
public class SecureSocketSession extends SocketSession {

    /**
     * Rehandshaking data may be mixed with application data.
     */
    private volatile boolean firstHandshake = true;
    private volatile boolean handshakeCompleted = false;

    /**
     * If the first handshake not completed, write message to this queue. When handshake completed then write to writeQueue
     */
    private final Queue tempWriteQueue = QueueFactory.createQueue();

    private boolean useClientMode = true;

    private SSLContext sslc;
    private SSLEngine engine;
    private ByteBuffer decodedReadBuffer; //save the plain text decoded from the readBuffer
    private ByteBuffer encodedWriteBuffer; //save the encoded text which encoded from the write message

    /**
     * Set the SSLContext which secure socket session will used.
     * 
     * @param sslc
     * 		SSLContext
     * @throws IllegalStateException 
     */
    public void setSSLContext(SSLContext sslc) throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set SSLContext after session have started");
        this.sslc = sslc;
    }

    /**
     * Get the SSLContext which secure socket session will used.
     * 
     * @return
     * 		used SSLContext
     */
    public SSLContext getSSLContext() {
        return sslc;
    }

    public void setUseClientMode(boolean b) throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set useClientMode after session have started");
        useClientMode = b;
    }

    public boolean isHandshakeCompleted() {
        return handshakeCompleted;
    }

    public synchronized void start(boolean block) throws IllegalStateException {
        if (sslc == null)
            throw new IllegalStateException("SSLContext is null");
        super.start(block);
    }

    protected void dispatchMessageSent(Message message) {
        if (!(message instanceof HandshakeMessage))
            super.dispatchMessageSent(message);
    }

    protected void dispatchSessionEstablished() {
        super.dispatchSessionEstablished();
        initSSLEngine();
        initBuffer();
        doHandshake();
    }

    /**
     * Subclass can orverride this method to config SSLEngine
     */
    protected void initSSLEngine() {
        Socket socket = getChannel().socket();
        engine = useClientMode ? sslc.createSSLEngine(socket.getInetAddress()
                .getHostAddress(), socket.getPort()) : sslc.createSSLEngine();
        engine.setUseClientMode(useClientMode);
    }

    private void initBuffer() {
        SSLSession session = engine.getSession();
        if (session.getPacketBufferSize() > readBuffer.capacity())
            readBuffer = ByteBufferUtils.increaseCapacity(readBuffer, session
                    .getPacketBufferSize()
                    - readBuffer.capacity());
        decodedReadBuffer = ByteBufferUtils.allocate(session
                .getApplicationBufferSize(), Constants.USE_DIRECT_BUFFER);
        encodedWriteBuffer = ByteBufferUtils.allocate(session
                .getPacketBufferSize(), Constants.USE_DIRECT_BUFFER);
    }

    /**
     * Handshake or Re-handshake
     */
    public void handshake() {
        if (!handshakeCompleted)
            return;
        doHandshake();
    }

    private void doHandshake() {
        try {
            handshakeCompleted = false;
            engine.beginHandshake();
            if (useClientMode) //otherwise wait received message
                super.write(new HandshakeMessage());
        } catch (SSLException e) {
            dispatchException(e);
            close();
        }
    }

    /**
     * First encode the message.toByteBuffer to encoedWriteBuffer, then
     * get content from the encodedWriteBuffer.
     * 
     * @return
     * 		encoded buffer
     */
    private ByteBuffer getContentFromEncodedWriteBuffer() {
        if (encodedWriteBuffer.hasRemaining()) {
            byte[] b = new byte[encodedWriteBuffer.remaining()];
            encodedWriteBuffer.get(b);
            encodedWriteBuffer.clear(); //then can encode new buffer
            return ByteBuffer.wrap(b);
        }
        return null;
    }

    /**
     * If the first handshake not completed, write to temp queue, wait handshake completed.
     * 
     * @param message
     * 		Write message
     * @throws IllegalArgumentException
     */
    public void write(Message message) throws IllegalArgumentException {
        if (firstHandshake)
            tempWriteQueue.push(message);
        else
            super.write(message);
    }

    public boolean blockWrite(Message message) throws IllegalArgumentException,
            IllegalStateException {
        if (isHandshakeCompleted())
            return super.blockWrite(message);
        else
            throw new IllegalStateException(
                    "can't block write until handshake completed");
    }

    protected synchronized void handshakeCompleted() {
        handshakeCompleted = true;
        if (firstHandshake) {
            firstHandshake = false;
            while (true) {
                Message message = (Message) tempWriteQueue.pop();
                if (message == null)
                    break;
                write(message);
            }
        }
    }

    protected void onUnregister() {
        if (engine != null) { //Close SSLEngine
            engine.closeOutbound();
            if (!engine.isInboundDone()) {
                ByteBuffer[] closeBuffer = messageToByteBuffer(new HandshakeMessage());
                try {
                    while (ByteBufferUtils.hasRemaining(closeBuffer)) {
                        long n = getChannel().write(closeBuffer);
                        if (n == -1)
                            break;
                    }
                } catch (IOException e) {
                }
            }
            try {
                engine.closeInbound();
            } catch (SSLException e) {
            }
            engine = null;
        }
        firstHandshake = true;
        handshakeCompleted = false;
        tempWriteQueue.clear();
        super.onUnregister();
    }

    protected ByteBuffer[] messageToByteBuffer(Message message) {
        ByteBuffer[] buffer = super.messageToByteBuffer(message);
        if (buffer == null)
            buffer = new ByteBuffer[0];
        try {
            while (true) {
                SSLEngineResult result = engine
                        .wrap(buffer, encodedWriteBuffer);
                Status status = result.getStatus();
                HandshakeStatus handshakeStatus = result.getHandshakeStatus();
                if (status == Status.OK) {
                    if (handshakeStatus == HandshakeStatus.NOT_HANDSHAKING) { //Not handshaking
                        break;
                    } else if (handshakeStatus == HandshakeStatus.NEED_UNWRAP) { //Should wait some received data
                        break;
                    } else if (handshakeStatus == HandshakeStatus.NEED_WRAP) { //Should wrap again
                        continue;
                    } else if (handshakeStatus == HandshakeStatus.NEED_TASK) {
                        engine.getDelegatedTask().run();
                    } else if (handshakeStatus == HandshakeStatus.FINISHED) {
                        handshakeCompleted();
                        break;
                    }
                } else if (status == Status.BUFFER_OVERFLOW) { //Increase buffer capacity
                    encodedWriteBuffer = ByteBufferUtils.increaseCapacity(
                            encodedWriteBuffer, Constants.BUFFER_CAPACITY);
                } else if (status == Status.CLOSED) {
                    break;
                } else if (status == Status.BUFFER_UNDERFLOW) { //Will never happen
                    close(false);
                    return null;
                }
            }
            encodedWriteBuffer.flip();
            return new ByteBuffer[] { getContentFromEncodedWriteBuffer() };
        } catch (SSLException e) {
            dispatchException(e);
            close();
            return null;
        }
    }

    /**
     * Decoded plain text from stream and then recognize
     * 
     * @param buffer
     * 		read buffer
     */
    protected void recognizeMessageAndDispatch(ByteBuffer buffer) {
        try {
            boolean recognized = false;
            boolean close = false;
            while (true) {
                SSLEngineResult result = engine.unwrap(buffer,
                        decodedReadBuffer);
                Status status = result.getStatus();
                HandshakeStatus handshakeStatus = result.getHandshakeStatus();
                if (status == Status.OK) {
                    if (handshakeStatus == HandshakeStatus.NOT_HANDSHAKING) { //Recognized some message
                        recognized = true;
                    } else if (handshakeStatus == HandshakeStatus.NEED_TASK) {
                        engine.getDelegatedTask().run();
                    } else if (handshakeStatus == HandshakeStatus.NEED_UNWRAP) {

                    } else if (handshakeStatus == HandshakeStatus.NEED_WRAP) { //Need write something to network
                        super.write(new HandshakeMessage());
                        break;
                    } else if (handshakeStatus == HandshakeStatus.FINISHED) {
                        handshakeCompleted();
                        break;
                    }
                } else if (status == Status.BUFFER_UNDERFLOW) { //Need more data, wait next time read
                    break;
                } else if (status == Status.BUFFER_OVERFLOW) {//Increase buffer capacity
                    decodedReadBuffer = ByteBufferUtils.increaseCapacity(
                            decodedReadBuffer, Constants.BUFFER_CAPACITY);
                } else if (status == Status.CLOSED) {
                    close = true;
                    break;
                }
            }
            if (recognized) {
                decodedReadBuffer.flip();
                super.recognizeMessageAndDispatch(decodedReadBuffer);
                decodedReadBuffer.compact();
            }
            if (close)
                close();
        } catch (SSLException e) {
            dispatchException(e);
            close();
        }
    }

    private static class HandshakeMessage implements Message {

        public boolean readFromBuffer(ByteBuffer buffer) {
            return false;
        }

        public ByteBuffer[] toByteBuffer() {
            return null;
        }
    }
}