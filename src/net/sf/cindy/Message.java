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
package net.sf.cindy;

import java.nio.ByteBuffer;

/**
 * Application message.
 * 
 * <pre>
 *                         Application
 *       
 *                     Message     Message
 *                        |           ^
 *     Session.write()    |     |     |   MessageRecognizer.recognizer()
 * Message.toByteBuffer() v     |     |   Message.readFromBuffer()
 *                  +-----+-----|-----+-----+
 *                  |                       |
 *                  |        Session        | 
 *                  |                       |
 *                  +-----+-----|-----+-----+
 *                        |     |     ^
 *                        |     |     |
 *                        v           |
 *      
 *                           Network
 * </pre>
 * 
 * @author Roger Chen
 */
public interface Message {

    /**
     * Read from buffer, when 
     * {@link net.sf.cindy.MessageRecognizer MessageRecognizer} recognize a 
     * message, sesssion will invoke this method to read content from buffer.
     * 
     * @param buffer 
     * 		read buffer cache
     * @return 
     * 		is read completed
     */
    boolean readFromBuffer(ByteBuffer buffer);

    /**
     * To ByteBuffer, {@link net.sf.cindy.Session Session} will invoke this 
     * method to write the message to network.
     * <p>
     * For reduce memory cost, the return type is ByteBuffer[]. For 
     * example, if you have such a message:
     * <p>
     * <pre>
     *     class ExampleMessage implement Message {
     *     		int header;
     * 			byte[] body; 
     *          ...
     *     }
     * </pre>  
     * <p>
     * you can just return a array contain two ByteBuffer,
     * and the second is ByteBuffer.wrap(body). If you only
     * need to return a ByteBuffer, you can return
     * new ByteBuffer[] {originalByteBuffer}.
     * 
     * @return 
     * 		ByteBuffer array
     */
    ByteBuffer[] toByteBuffer();
}