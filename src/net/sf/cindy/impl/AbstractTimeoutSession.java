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

/**
 * Added check timeout support.
 * 
 * @author Roger Chen
 */
public abstract class AbstractTimeoutSession extends AbstractSession {

    private boolean established = false;

    private int idleTimes = 0; //Session idle time

    protected void dispatchSessionEstablished() {
        established = true;
        super.dispatchSessionEstablished();
    }

    protected void dispatchSessionClosed() {
        established = false;
        idleTimes = 0;
        super.dispatchSessionClosed();
    }

    public void onEvent(Object event, Object attachment) {
        if (event == Constants.EV_CHECK_TIMEOUT)
            checkTimeout(((Integer) attachment).intValue());
        else if (event == Constants.EV_EVENT_HAPPEN)
            eventHappen();
        super.onEvent(event, attachment);
    }

    private void checkTimeout(int interval) {
        if (!established)
            return;
        if (getSessionTimeout() > 0) {
            idleTimes += interval;
            if (idleTimes >= getSessionTimeout()) {
                dispatchSessionTimeout();
                idleTimes = 0;//prevent timeout event repeat frequently
            }
        }
        if (writeQueue.isEmpty()) {
            dispatchSessionIdle();
        }
    }

    private void eventHappen() {
        idleTimes = 0;
    }
}