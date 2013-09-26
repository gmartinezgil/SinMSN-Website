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

import net.sf.cindy.EventGenerator;

/**
 * JMX support interface.
 * 
 * @see net.sf.cindy.EventGenerator
 * 
 * @author Roger Chen
 */
public interface SimpleEventGeneratorMBean extends EventGenerator {

    /**
     * Get event generator id. The id must be unique in current class loader.
     * 
     * @return
     * 		id
     */
    public int getId();

    /**
     * Get thread priority.
     * 
     * @return
     * 		priority
     */
    public int getPriority();

    /**
     * Set thread priority.
     * 
     * @param priority
     * 		thread priority
     * @throws IllegalArgumentException
     */
    public void setPriority(int priority) throws IllegalArgumentException;
}