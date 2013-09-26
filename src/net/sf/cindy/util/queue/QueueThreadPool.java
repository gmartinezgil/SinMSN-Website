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
 * {@link net.sf.cindy.util.queue.QueueThread QueueThread} pool.
 * 
 * @author Roger Chen
 */
public interface QueueThreadPool {

    /**
     * add an object to pool. The object can't be null.
     * 
     * @param obj
     * 		the object
     */
    public void add(Object obj);

    /**
     * clear all objects in pool.
     */
    public void clear();

    /**
     * get all objects size in pool.
     * 
     * @return 
     * 		objects size
     */
    public int getSize();

    /**
     * set thread pool size.
     * 
     * @param threadPoolSize
     *      thread pool size
     * @throws IllegalArgumentException
     * 		if threadPoolSize is negative
     */
    public void setThreadPoolSize(int threadPoolSize)
            throws IllegalArgumentException;

    /**
     * get thread pool size.
     * 
     * @return 
     * 		thread pool size
     */
    public int getThreadPoolSize();

}