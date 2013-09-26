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
import java.nio.channels.ClosedSelectorException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Set;

import net.sf.cindy.Session;
import net.sf.cindy.spi.EventGeneratorSpi;
import net.sf.cindy.spi.SessionSpi;
import net.sf.cindy.util.ElapsedTime;
import net.sf.cindy.util.Utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple event generator, manage event and event dispatch. It's just 
 * generate event, Session implementations should listen event and do
 * what they what to do. 
 * 
 * <pre>
 * 
 *           EventGenerator                
 *       +------>------>------+                      
 *       |                    |    Event                      
 *       ^  Selector.select() v----------->Session.onEvent()              
 *       |                    |   
 *       +------<------<------+
 * 
 * </pre>
 * 
 * @author Roger Chen
 */
public class SimpleEventGenerator implements EventGeneratorSpi,
        SimpleEventGeneratorMBean {

    private static final Log log = LogFactory
            .getLog(SimpleEventGenerator.class);

    private static int i = 0; //Used for build thread name
    private final int id = i++;

    private final Collection register = new LinkedList();
    private Thread thread = null;
    private Selector selector;

    private volatile boolean close = false;

    /**
     * The last select time, for judge session timeout.
     */
    private ElapsedTime lastSelectTime;

    private int priority = Thread.NORM_PRIORITY;

    public int getId() {
        return id;
    }

    public int getPriority() {
        return priority;
    }

    public void setPriority(int priority) throws IllegalArgumentException {
        if (priority < Thread.MIN_PRIORITY || priority > Thread.MAX_PRIORITY)
            throw new IllegalArgumentException();
        if (thread != null)
            thread.setPriority(priority);
        this.priority = priority;
    }

    public boolean isStarted() {
        return thread != null;
    }

    public synchronized void start() {
        if (isStarted()) {
            close = false; //if current thread close and restart, just set close to false
            return;
        }
        doStart();
    }

    protected void doStart() {
        try {
            if (log.isTraceEnabled())
                log.trace("start EventGenerator " + id);
            lastSelectTime = null;
            register.clear();
            close = false;
            selector = Selector.open();
            thread = new Thread() {

                public void run() {
                    SimpleEventGenerator.this.run();
                }
            };
            thread.setName(Utils.getClassSimpleName(getClass()) + id);
            thread.setPriority(priority);
            thread.start();
        } catch (IOException e) {
            log.error(e, e);
        }
    }

    public synchronized void close() {
        stop();
    }

    public synchronized void stop() {
        if (!isStarted())
            return;
        doStop();
    }

    protected void doStop() {
        if (log.isTraceEnabled())
            log.trace("stop EventGenerator " + id);
        close = true;
        selector.wakeup();
        if (!isEventGeneratorThread()) { //Wait until finished
            while (thread.isAlive()) {
                try {
                    thread.join();
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public boolean isEventGeneratorThread() {
        return Thread.currentThread() == thread;
    }

    public void register(SessionSpi session, Object event) {
        if (isEventGeneratorThread())
            session.onEvent(event, selector);
        else {
            synchronized (register) {
                register.add(new Object[] { session, event });
            }
            selector.wakeup();
        }
    }

    private void run() {
        try {
            while (!close) {
                beforeSelect(selector);
                if (close)
                    break;

                int readyKeyCount = 0;
                try {
                    readyKeyCount = selector
                            .select(Constants.CHECK_SESSION_TIMEOUT_INTERVAL);
                } catch (ClosedSelectorException cse) {
                    break;
                } catch (IOException e) {
                    log.error(e, e);
                    break;
                }
                afterSelect(selector);

                if (readyKeyCount > 0) {
                    for (Iterator iter = selector.selectedKeys().iterator(); iter
                            .hasNext();) {
                        SelectionKey key = (SelectionKey) iter.next();
                        iter.remove();

                        try {
                            processKey(key);
                        } catch (Exception e) { //Protection catch
                            ((Session) key.attachment()).dispatchException(e);
                        }
                    }
                }
            }
        } finally {
            finishedSelect(selector);
        }
    }

    protected void processKey(SelectionKey key) {
        SessionSpi session = (SessionSpi) key.attachment();

        if (key.isAcceptable())
            session.onEvent(Constants.EV_ACCEPTABLE, null);
        if (key.isConnectable())
            session.onEvent(Constants.EV_CONNECTABLE, null);
        if (key.isValid() && key.isReadable())
            session.onEvent(Constants.EV_READABLE, null);
        if (key.isValid() && key.isWritable())
            session.onEvent(Constants.EV_WRITABLE, null);
    }

    protected void beforeSelect(Selector selector) {
        changeRegisterChannel();
    }

    private void changeRegisterChannel() {
        Object[] objects = null;
        synchronized (register) {
            objects = new Object[register.size()];
            register.toArray(objects);
            register.clear();
        }

        for (int i = 0; i < objects.length; i++) {
            Object[] object = (Object[]) objects[i];
            SessionSpi session = (SessionSpi) object[0];
            session.onEvent(object[1], selector);
        }
    }

    protected void afterSelect(Selector selector) {
        checkSessionTimeout();
    }

    private void checkSessionTimeout() {
        if (lastSelectTime == null) {
            lastSelectTime = new ElapsedTime();
            return;
        }

        Set selectedKeys = selector.selectedKeys();
        for (Iterator iter = selectedKeys.iterator(); iter.hasNext();) {
            SelectionKey key = (SelectionKey) iter.next();
            SessionSpi session = (SessionSpi) key.attachment();
            session.onEvent(Constants.EV_EVENT_HAPPEN, null);
        }

        //imporve performance, do not check session timeout frequently
        if (lastSelectTime.getElapsedTime() >= Constants.CHECK_SESSION_TIMEOUT_INTERVAL) {
            Integer interval = new Integer((int) lastSelectTime.reset());
            for (Iterator iter = selector.keys().iterator(); iter.hasNext();) {
                SelectionKey key = (SelectionKey) iter.next();
                if (!selectedKeys.contains(key)) { //no event happen, check timeout
                    SessionSpi session = (SessionSpi) key.attachment();
                    session.onEvent(Constants.EV_CHECK_TIMEOUT, interval);
                }
            }
        }
    }

    protected void finishedSelect(Selector selector) {
        for (Iterator iter = selector.keys().iterator(); iter.hasNext();) {
            SelectionKey key = (SelectionKey) iter.next();
            Session session = (Session) key.attachment();
            session.close();
        }
        try {
            selector.close();
        } catch (IOException e) {
        }
        selector = null;
        lastSelectTime = null;
        register.clear();
        close = false;
        thread = null;
    }

}