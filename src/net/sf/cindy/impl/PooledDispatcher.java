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

import net.sf.cindy.Session;
import net.sf.cindy.spi.DispatcherSpi;
import net.sf.cindy.util.Utils;
import net.sf.cindy.util.queue.DefaultQueueThreadPool;
import net.sf.cindy.util.queue.QueueThread;

/**
 * Pooled dispatcher, the session listener event may be dispatched not in 
 * the original order.
 * 
 * @author Roger Chen
 */
public class PooledDispatcher implements DispatcherSpi, PooledDispatcherMBean {

    private static int i = 0;

    private final boolean daemon;
    private final DispatcherSpi dispatcher = new SimpleDispatcher();
    private final DefaultQueueThreadPool pool;

    public PooledDispatcher() {
        this(true);
    }

    public PooledDispatcher(boolean daemon) {
        this(daemon, 0);
    }

    public PooledDispatcher(boolean daemon, int poolSize) {
        this.daemon = daemon;
        pool = initPool();
        setThreadPoolSize(poolSize);
    }

    protected DefaultQueueThreadPool initPool() {
        return new DispatchThreadPool();
    }

    public void dispatch(Session session, Runnable runnable) {
        pool.add(new Object[] { session, runnable });
    }

    public int getThreadPoolSize() {
        return pool.getThreadPoolSize();
    }

    public void setThreadPoolSize(int poolSize) {
        pool.setThreadPoolSize(poolSize);
    }

    protected class DispatchThreadPool extends DefaultQueueThreadPool {

        protected QueueThread newQueueThread() {
            QueueThread thread = super.newQueueThread();
            thread.setDaemon(daemon);
            thread.setName(Utils.getClassSimpleName(PooledDispatcher.this
                    .getClass())
                    + "-" + ++i);
            return thread;
        }

        protected void action(Object obj) {
            Object[] objArray = (Object[]) obj;
            dispatcher.dispatch((Session) objArray[0], (Runnable) objArray[1]);
        }
    }
}