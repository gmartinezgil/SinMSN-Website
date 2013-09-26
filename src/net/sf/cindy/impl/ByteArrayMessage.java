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
import java.nio.ByteBuffer;

/**
 * Byte array message.
 * 
 * @author Roger Chen
 */
public final class ByteArrayMessage extends AbstractPacketMessage {

    private byte[] content;

    public ByteArrayMessage() {
    }

    public ByteArrayMessage(byte[] content) {
        setContent(content);
    }

    public ByteArrayMessage(byte[] content, SocketAddress address) {
        setContent(content);
        setSocketAddress(address);
    }

    public byte[] getContent() {
        return content;
    }

    public void setContent(byte[] content) {
        this.content = content;
    }

    public boolean readFromBuffer(ByteBuffer buffer) {
        content = new byte[buffer.remaining()];
        buffer.get(content);
        return true;
    }

    public ByteBuffer[] toByteBuffer() {
        if (content != null && content.length > 0) {
            return new ByteBuffer[] { ByteBuffer.wrap(content) };
        }
        return null;
    }

    public String toString() {
        if (content == null)
            return "";
        return new String(content);
    }

}