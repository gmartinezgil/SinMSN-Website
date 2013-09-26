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

import net.sf.cindy.SessionStatistic;

/**
 * JMX support interface. 
 * 
 * @see net.sf.cindy.Session
 * 
 * @author Roger Chen
 */
public interface SessionMBean {

    public int getId();

    public SessionStatistic getStatistic();

    public void setSessionTimeout(int timeout);

    public int getSessionTimeout();

    public boolean isLogException();

    public void setLogException(boolean logException);

    public int getBufferCapacityLimit();

    public void setBufferCapacityLimit(int bufferCapacityLimit);

    public void start(boolean block) throws IllegalStateException;

    public void start() throws IllegalStateException;

    public void close(boolean block);

    public void close();

    public boolean isClosing();

    public boolean isStarted();

    public boolean isAvailable();

    public int getWriteQueueSize();
}