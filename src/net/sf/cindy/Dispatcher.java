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
 * Dispatch {@link net.sf.cindy.SessionListener SessionListener} event and
 * {@link net.sf.cindy.SessionStatisticListener SessionStatisticListener} event.
 * <p>
 * Dispatcher implemetations may be a thread or thread pool, dispatch event 
 * in an other thread.
 * 
 * @author Roger Chen
 */
public interface Dispatcher {

}