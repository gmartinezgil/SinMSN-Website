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
 * Message Recognizer, recognize {@link net.sf.cindy.Message Message}.
 * 
 * @author Roger Chen
 */
public interface MessageRecognizer {

    /**
     * Judge {@link net.sf.cindy.Message Message} type and return a instance 
     * of that message. If the message is not completed, SHOULD return null. 
     * If the message is unknown, SHOULD ignore the wrong bytes or close 
     * the session.
     * 
     * @param session 
     * 		The session which read some data 
     * @param buffer 
     * 		the readonly slice ByteBuffer
     * @return 
     * 		The message instance
     */
    Message recognize(Session session, ByteBuffer buffer);
}