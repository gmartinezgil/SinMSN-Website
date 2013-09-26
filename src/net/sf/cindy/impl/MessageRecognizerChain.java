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

import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.Iterator;

import net.sf.cindy.Message;
import net.sf.cindy.MessageRecognizer;
import net.sf.cindy.Session;
import net.sf.cindy.util.CopyOnWriteCollection;

/**
 * MessageRecognizer chain.
 * 
 * @author Roger Chen
 */
public class MessageRecognizerChain implements MessageRecognizer {

    private final Collection chain = new CopyOnWriteCollection();

    /**
     * Add a MessageRecognizer to the chain.
     * 
     * @param recognizer 
     * 		the MessageRecognizer
     */
    public void addMessageRecognizer(MessageRecognizer recognizer) {
        if (recognizer != null)
            chain.add(recognizer);
    }

    /**
     * Remove a MessageRecognizer from the chain.
     * 
     * @param recognizer
     * 		the MessageRecognizer
     */
    public void removeMessageRecognizer(MessageRecognizer recognizer) {
        if (recognizer != null)
            chain.remove(recognizer);
    }

    public Message recognize(Session session, ByteBuffer buffer) {
        for (Iterator iter = chain.iterator(); iter.hasNext();) {
            MessageRecognizer recognizer = (MessageRecognizer) iter.next();
            Message message = recognizer.recognize(session, buffer.slice());
            if (message != null) {
                return message;
            }
        }
        return null;
    }

}