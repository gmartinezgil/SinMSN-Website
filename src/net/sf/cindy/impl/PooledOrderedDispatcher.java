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

import java.util.Map;
import java.util.WeakHashMap;

import net.sf.cindy.Session;
import net.sf.cindy.util.queue.DefaultQueueThreadPool;
import net.sf.cindy.util.queue.QueueThread;

/**
 * Pooled ordered dispatcher, the session listener event will be dispatched 
 * in the original order.
 * 
 * @author Roger Chen
 */
public class PooledOrderedDispatcher extends PooledDispatcher {

    private final Map sessionMap = new WeakHashMap(); //Key=Session£¬Value=Thread

    public PooledOrderedDispatcher() {
    }

    public PooledOrderedDispatcher(boolean daemon) {
        super(daemon);
    }

    public PooledOrderedDispatcher(boolean daemon, int poolSize) {
        super(daemon, poolSize);
    }

    protected DefaultQueueThreadPool initPool() {
        return new OrderedDispatchThreadPool();
    }

    private class OrderedDispatchThreadPool extends DispatchThreadPool {

        public void add(Object obj) {
            Session session = (Session) ((Object[]) obj)[0];
            QueueThread queueThread = null;
            synchronized (sessionMap) {
                queueThread = (QueueThread) sessionMap.get(session);
                if (queueThread == null) {
                    queueThread = chooseThread();
                    sessionMap.put(session, queueThread);
                }
            }
            if (queueThread != null)
                queueThread.add(obj);
        }
    }

}