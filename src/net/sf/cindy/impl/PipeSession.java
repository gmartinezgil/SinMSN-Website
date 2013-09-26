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
import java.nio.channels.ClosedChannelException;
import java.nio.channels.Pipe;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;

/**
 * Pipe session.
 * 
 * @author Roger Chen
 */
public class PipeSession extends StreamChannelSession {

    private Pipe pipe;

    /**
     * Set the pipe which the session will used.
     * 
     * @param pipe
     * 		pipe
     * @throws IllegalStateException
     */
    public void setPipe(Pipe pipe) throws IllegalStateException {
        if (isStarted())
            throw new IllegalStateException(
                    "can't set pipe after session started");
        this.pipe = pipe;
    }

    /**
     * Get pipe associted with the session.
     * 
     * @return
     * 		pipe
     */
    public Pipe getPipe() {
        return pipe;
    }

    public synchronized void start(boolean block) throws IllegalStateException {
        if (isStarted())
            return;
        if (pipe == null) {
            try {
                pipe = Pipe.open();
            } catch (IOException e) {
                dispatchException(e);
                dispatchSessionClosed();
                return;
            }
        }
        startSession(pipe.source(), pipe.sink(), block);
    }

    protected void onRegister(Selector selector) {
        try {
            pipe.sink().register(selector, 0, this);
            pipe.source().register(selector, SelectionKey.OP_READ, this);
            super.onRegister(selector);
            dispatchSessionEstablished();
        } catch (ClosedChannelException e) {
            close();
        }
    }

    protected void onUnregister() {
        pipe = null;
        super.onUnregister();
    }

}