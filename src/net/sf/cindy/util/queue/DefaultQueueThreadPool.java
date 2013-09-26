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
package net.sf.cindy.util.queue;

/**
 * Default queue thread pool. Each 
 * {@link net.sf.cindy.util.queue.QueueThread QueueThread} in pool have
 * a {@link net.sf.cindy.util.queue.BlockingQueue BlockingQueue}.
 * 
 * @author Roger Chen
 */
public abstract class DefaultQueueThreadPool implements QueueThreadPool {

    private QueueThread[] threads = new QueueThread[0];
    private int pos = -1; //Current position

    public void setThreadPoolSize(int threadPoolSize)
            throws IllegalArgumentException {
        if (threadPoolSize < 0)
            throw new IllegalArgumentException(
                    "thread pool size can't be negative");
        synchronized (threads) {
            int diff = threadPoolSize - threads.length;
            if (diff == 0)
                return;
            QueueThread[] newThreads = new QueueThread[threadPoolSize];
            if (diff > 0) {
                System.arraycopy(threads, 0, newThreads, 0, threads.length);
                for (int i = 0; i < diff; i++) {
                    QueueThread thread = newQueueThread();
                    newThreads[threads.length + i] = thread;
                    thread.start();
                }
                threads = newThreads;
            } else {
                System.arraycopy(threads, 0, newThreads, 0, newThreads.length);
                diff = -diff;
                BlockingQueue[] queues = new BlockingQueue[diff];
                for (int i = 0; i < diff; i++) {
                    QueueThread queueThread = threads[threads.length - 1 - i];
                    queues[i] = queueThread.stopRunImmediately();
                }
                threads = newThreads;
                //add remain objects
                for (int i = 0; i < queues.length; i++) {
                    int size = queues[i].size();
                    for (int j = 0; j < size; j++) {
                        add(queues[i].pop());
                    }
                }
            }
        }
    }

    public int getThreadPoolSize() {
        synchronized (threads) {
            return threads.length;
        }
    }

    protected QueueThread chooseThread() {
        synchronized (threads) {
            if (threads.length > 0) {
                pos = (pos + 1) % threads.length;
                return threads[pos];
            }
            return null;
        }
    }

    public void add(Object obj) {
        QueueThread thread = chooseThread();
        if (thread != null) {
            thread.add(obj);
        } else
            action(obj);
    }

    public void clear() {
        synchronized (threads) {
            for (int i = 0; i < threads.length; i++) {
                threads[i].clear();
            }
        }
    }

    public int getSize() {
        int count = 0;
        synchronized (threads) {
            for (int i = 0; i < threads.length; i++) {
                count += threads[i].size();
            }
        }
        return count;
    }

    protected QueueThread newQueueThread() {
        return new QueueThread() {

            protected void action(Object obj) {
                DefaultQueueThreadPool.this.action(obj);
            }
        };
    }

    /**
     * Do action with added object.
     * 
     * @param obj
     * 		added object
     */
    protected abstract void action(Object obj);
}