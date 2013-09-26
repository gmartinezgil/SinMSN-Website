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
 * Default blocking queue implementation. 
 * 
 * @author Roger Chen
 */
class DefaultBlockingQueue implements BlockingQueue {

    private final Queue queue = QueueFactory.createQueue();

    public void push(Object obj) {
        queue.push(obj);
        synchronized (this) {
            notify();
        }
    }

    public Object pop() {
        while (true) {
            Object obj = queue.pop();
            if (obj != null) {
                return obj;
            } else {
                synchronized (this) {
                    try {
                        wait();
                    } catch (InterruptedException ie) {
                        return null;
                    }
                }
            }
        }
    }

    public Object peek() {
        return queue.peek();
    }

    public void clear() {
        queue.clear();
    }

    public boolean isEmpty() {
        return queue.isEmpty();
    }

    public int size() {
        return queue.size();
    }

}