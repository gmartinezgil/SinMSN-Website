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
import java.nio.ByteBuffer;
import java.nio.channels.GatheringByteChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.SelectableChannel;
import java.nio.channels.WritableByteChannel;

import net.sf.cindy.Message;
import net.sf.cindy.spi.EventGeneratorSpi;
import net.sf.cindy.spi.SessionStatisticSpi;
import net.sf.cindy.util.ByteBufferUtils;

/**
 * StreamChannel have the same behavior when reading and writing.
 * 
 * @author Roger Chen
 */
public abstract class StreamChannelSession extends ChannelSession {

    protected void readFromChannel(SelectableChannel channel)
            throws IOException {
        if (!readBuffer.hasRemaining()) { //have no room for contain new data
            readBuffer = ByteBufferUtils.increaseCapacity(readBuffer,
                    Constants.BUFFER_CAPACITY);
        }
        int n = -1;
        int readCount = 0;
        while ((n = ((ReadableByteChannel) channel).read(readBuffer)) > 0) {
            readCount += n;
        }

        if (readCount > 0) {
            if (getStatistic() != null)
                ((SessionStatisticSpi) getStatistic()).received(readCount);
            readBuffer.flip();
            recognizeMessageAndDispatch(readBuffer);
            readBuffer.compact();

            if (readBuffer.position() >= getBufferCapacityLimit()) {
                dispatchException(new RuntimeException(
                        "ReadBuffer reaches it's max capacity. To prevent attack, session will be closed."));
                close(false);
                return;
            }
        }
        if (n < 0) { //Connection closed
            channel.close();
            close();
        } else {
            ((EventGeneratorSpi) getEventGenerator()).register(this,
                    Constants.EV_ENABLE_READ);
        }
    }

    protected void recognizeMessageAndDispatch(ByteBuffer buffer) {
        while (buffer.hasRemaining()) {
            Message message = recognizeMessage(buffer);
            if (message == null)
                break;
            dispatchMessageReceived(message);
        }
    }

    protected final Object transMessage(Message message) {
        return new WriteMessage(message);
    }

    /**
     * convert Message to ByteBuffer[]. Normally return message.toByteBuffer,
     * but in some secure connection will return encoded content. 
     * 
     * @param message
     * 		Message
     * @return
     * 		converted ByteBuffer array
     */
    protected ByteBuffer[] messageToByteBuffer(Message message) {
        return message.toByteBuffer();
    }

    protected Message writeToChannel(SelectableChannel channel,
            Object writeMessage) throws IOException {
        WriteMessage message = (WriteMessage) writeMessage;
        if (message.buffer == null)
            message.buffer = messageToByteBuffer(message.message);

        ByteBuffer[] writeBuffer = message.buffer;
        if (writeBuffer == null || writeBuffer.length == 0)
            return message.message; //Write completed

        while (true) {
            long n = doRealWrite(channel, writeBuffer);
            if (getStatistic() != null)
                ((SessionStatisticSpi) getStatistic()).sent(n);

            if (!ByteBufferUtils.hasRemaining(writeBuffer))
                return message.message;
            else if (n == 0) {//have more data, but the buffer is full, wait next time to write
                return null;
            }
        }
    }

    private long doRealWrite(SelectableChannel channel, ByteBuffer[] buffer)
            throws IOException {
        // Now JDK 1.4 have a bug when use SocketChannel
        // or Pipe to write ByteBuffer array.
        if (Constants.SUPPORT_WRITE_BUFFER_ARRAY)
            return ((GatheringByteChannel) channel).write(buffer);

        long totalWriteCount = 0;
        for (int i = 0; i < buffer.length; i++) {
            if (buffer[i] != null)
                while (buffer[i].hasRemaining()) {
                    int writeCount = ((WritableByteChannel) channel)
                            .write(buffer[i]);
                    if (writeCount == 0) //maybe full
                        return totalWriteCount;
                    totalWriteCount += writeCount;
                }
        }
        return totalWriteCount;
    }

    /**
     * A message maybe write complete via several IO operate£¬ should save the status
     * 
     * @author Roger Chen
     */
    private static class WriteMessage {

        private Message message;
        private ByteBuffer[] buffer;

        private WriteMessage(Message message) {
            this.message = message;
        }

        public ByteBuffer[] getBuffer() {
            return buffer;
        }

        public Message getMessage() {
            return message;
        }
    }
}