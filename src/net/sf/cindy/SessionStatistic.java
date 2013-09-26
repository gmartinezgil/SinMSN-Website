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

/**
 * Session statistic.
 * 
 * @author Roger Chen
 */
public interface SessionStatistic {

    /**
     * Get the session associated with the session statistic.
     * 
     * @return
     * 		the session
     */
    public Session getSession();

    /**
     * Get received bytes count. If session restart, the value will reset to
     * 0.
     * 
     * @return
     * 		received bytes count
     */
    public long getReceivedBytes();

    /**
     * Get sent bytes count. If session restart, the value will reset to 0.
     * 
     * @return
     * 		sent bytes count
     */
    public long getSentBytes();

    /**
     * Get elapsed time after session started and before session closed, in ms.
     * 
     * @return
     * 		elapsed time 
     */
    public long getElapsedTime();

    /**
     * Get average receive speed, in byte/s.
     * 
     * @return
     * 		average receive speed.
     */
    public double getAvgReceiveSpeed();

    /**
     * Get average send speed, in byte/s.
     * 
     * @return
     * 		average send speed.
     */
    public double getAvgSendSpeed();

    /**
     * Get current receive speed, in byte/s.
     * 
     * @return
     * 		received speed
     */
    public double getReceiveSpeed();

    /**
     * Get current send speed, in byte/s.
     * 
     * @return
     * 		send speed
     */
    public double getSendSpeed();

    /**
     * Add session statistic listener.
     * 
     * @param listener
     * 		session statistic listener
     */
    public void addListener(SessionStatisticListener listener);

    /**
     * Remove session statistic listener.
     * 
     * @param listener
     * 		session statistic listener
     */
    public void removeListener(SessionStatisticListener listener);

}