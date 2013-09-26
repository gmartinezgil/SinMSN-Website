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
package net.sf.cindy.spi;

import net.sf.cindy.EventGenerator;

/**
 * {@link net.sf.cindy.EventGenerator EventGenerator} spi.
 * 
 * @author Roger Chen
 */
public interface EventGeneratorSpi extends EventGenerator {

    /**
     * Register an event.
     * 
     * @param session	
     * 		which session want to register an event
     * @param event
     * 		registered event
     */
    public void register(SessionSpi session, Object event);

    /**
     * Judge current thread is the EventGenerator thread.
     * 
     * @return
     * 		the current thread is the EventGenerator thread
     */
    public boolean isEventGeneratorThread();
}
