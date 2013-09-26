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

import java.util.Collection;
import java.util.Iterator;

import net.sf.cindy.Session;
import net.sf.cindy.SessionStatisticListener;
import net.sf.cindy.spi.DispatcherSpi;
import net.sf.cindy.spi.SessionStatisticSpi;
import net.sf.cindy.util.CopyOnWriteCollection;
import net.sf.cindy.util.Speed;

/**
 * Simple session statistic.
 * 
 * @author Roger Chen
 */
public class SimpleSessionStatistic implements SessionStatisticSpi {

    private final Session session;
    private final Speed received = Speed.getInstance();
    private final Speed sent = Speed.getInstance();

    private final Collection listeners = new CopyOnWriteCollection();

    private boolean stopped = true;
    private double avgReceiveSpeed = 0;
    private double avgSendSpeed = 0;
    private long elapsedTime = 0;

    public SimpleSessionStatistic(Session session) {
        this.session = session;
    }

    public Session getSession() {
        return session;
    }

    public double getAvgReceiveSpeed() {
        if (stopped)
            return avgReceiveSpeed;
        return received.getAvgSpeed();
    }

    public double getAvgSendSpeed() {
        if (stopped)
            return avgSendSpeed;
        return sent.getAvgSpeed();
    }

    public long getElapsedTime() {
        if (stopped)
            return elapsedTime;
        return received.getElapsedTime();
    }

    public long getReceivedBytes() {
        return received.getTotalValue();
    }

    public long getSentBytes() {
        return sent.getTotalValue();
    }

    public double getReceiveSpeed() {
        if (stopped)
            return 0;
        return received.getSpeed();
    }

    public double getSendSpeed() {
        if (stopped)
            return 0;
        return sent.getSpeed();
    }

    public void addListener(SessionStatisticListener listener) {
        if (listener != null)
            listeners.add(listener);
    }

    public void removeListener(SessionStatisticListener listener) {
        if (listener != null)
            listeners.remove(listener);
    }

    private void dispatch(Runnable runnable) {
        ((DispatcherSpi) session.getDispatcher()).dispatch(session, runnable);
    }

    public void received(final long bytes) {
        received.addValue(bytes);
        dispatch(new Runnable() {

            public void run() {
                for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                    SessionStatisticListener listener = (SessionStatisticListener) iter
                            .next();
                    listener.received(SimpleSessionStatistic.this, bytes);
                }
            }
        });
    }

    public void sent(final long bytes) {
        sent.addValue(bytes);
        dispatch(new Runnable() {

            public void run() {
                for (Iterator iter = listeners.iterator(); iter.hasNext();) {
                    SessionStatisticListener listener = (SessionStatisticListener) iter
                            .next();
                    listener.sent(SimpleSessionStatistic.this, bytes);
                }
            }
        });
    }

    public synchronized void start() {
        avgReceiveSpeed = 0;
        avgSendSpeed = 0;
        elapsedTime = 0;
        received.reset();
        sent.reset();
        stopped = false;
    }

    public synchronized void stop() {
        avgReceiveSpeed = received.getAvgSpeed();
        avgSendSpeed = sent.getAvgSpeed();
        elapsedTime = received.getElapsedTime();
        stopped = true;
    }
}