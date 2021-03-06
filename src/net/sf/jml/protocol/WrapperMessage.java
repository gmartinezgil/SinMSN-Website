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
package net.sf.jml.protocol;

import java.nio.ByteBuffer;

import net.sf.cindy.Message;

/**
 * Used for decoupling jml and cindy.
 * 
 * @author Roger Chen
 */
public final class WrapperMessage implements Message {

    private final MsnMessage message;

    public WrapperMessage(MsnMessage message) {
        this.message = message;
    }

    public MsnMessage getMessage() {
        return message;
    }

    public boolean readFromBuffer(ByteBuffer buffer) {
        return message.load(buffer);
    }

    public ByteBuffer[] toByteBuffer() {
        return message.save();
    }

    @Override
	public String toString() {
        return message.toString();
    }
}