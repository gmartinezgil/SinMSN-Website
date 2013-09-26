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

import net.sf.cindy.Message;
import net.sf.cindy.PacketMessage;

/**
 * PacketChannelSession have the same behavior when reading and writing.
 * 
 * @author Roger Chen
 */
public abstract class PacketChannelSession extends ChannelSession {

    protected SocketAddress lastSocketAddress;

    protected final Message recognizeMessage(ByteBuffer buffer) {
        Message message = super.recognizeMessage(buffer);
        if (message != null && message instanceof PacketMessage)
            ((PacketMessage) message).setSocketAddress(lastSocketAddress);
        return message;
    }

}