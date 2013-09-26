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

import java.nio.channels.Selector;

import net.sf.cindy.spi.SessionSpi;

/**
 * When no session avaiable, will auto close.
 * 
 * @author Roger Chen
 */
public class AutoCloseEventGenerator extends SimpleEventGenerator {

    private boolean addedSession = false;

    protected void doStart() {
        addedSession = false;
        super.doStart();
    }

    public void register(SessionSpi session, Object event) {
        super.register(session, event);
        addedSession = true;
    }

    protected void beforeSelect(Selector selector) {
        super.beforeSelect(selector);
        if (addedSession && selector.keys().size() == 0)
            stop();
    }
}