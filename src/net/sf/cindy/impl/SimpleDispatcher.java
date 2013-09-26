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

import net.sf.cindy.Session;
import net.sf.cindy.spi.DispatcherSpi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Simple dispatcher, run in the same thread as the invoker. 
 * 
 * @author Roger Chen
 */
public class SimpleDispatcher implements DispatcherSpi, SimpleDispatcherMBean {

    private static final Log log = LogFactory.getLog(SimpleDispatcher.class);

    public void dispatch(Session session, Runnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) { // protection catch
            log.error(e, e);
        }
    }

}