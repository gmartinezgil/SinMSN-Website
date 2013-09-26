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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Queue thread. Action added object in sequence.
 * 
 * @author Roger Chen
 */
public abstract class QueueThread extends Thread {

    private static final Log log = LogFactory.getLog(QueueThread.class);

    private final BlockingQueue queue = QueueFactory.createBlockingQueue();
    private final Object lockObject = new Object();

    private volatile boolean stop = false;

    /**
     * The queue thread is empty.
     * 
     * @return
     * 		the queue thread is empty
     */
    public boolean isEmpty() {
        return queue.isEmpty();
    }

    /**
     * Add a object to the queue thread.
     * 
     * @param obj
     * 		the object
     */
    public void add(Object obj) {
        queue.push(obj);
    }

    /**
     * Clear current queue thread.
     */
    public void clear() {
        queue.clear();
    }

    /**
     * Get current queue thread size.
     * 
     * @return
     * 		current queue thread size
     */
    public int size() {
        return queue.size();
    }

    public void run() {
        while (true) {
            try {
                if (stop) {
                    synchronized (lockObject) {
                        lockObject.notify();
                    }
                    break;
                }
                Object obj = queue.pop();
                if (obj != null) {
                    action(obj);
                }
            } catch (Throwable e) {
                log.error(e, e);
            }
        }
        stop = true;
    }

    /**
     * Do action with added object.
     * 
     * @param obj
     * 		added object
     */
    protected abstract void action(Object obj);

    /**
     * Stop run. When the method returns, current queue thread have
     * stopped.
     * 
     * @return
     * 		the blocking queue in current thread
     */
    public BlockingQueue stopRunImmediately() {
        synchronized (lockObject) {
            stop = true;
            interrupt();
            if (Thread.currentThread() != this)
                try {
                    lockObject.wait();
                } catch (InterruptedException e) {
                }
            return queue;
        }
    }

}