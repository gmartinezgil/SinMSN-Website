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
package net.sf.cindy.util.queue;

import java.util.LinkedList;
import java.util.List;

/**
 * Simple queue. All methods are thread-safe.
 * 
 * @author Roger Chen
 */
class SimpleQueue implements Queue {

    private final List list = new LinkedList();

    public synchronized void push(Object obj) {
        if (obj != null) {
            list.add(obj);
        }
    }

    public synchronized Object pop() {
        if (!isEmpty()) {
            return list.remove(0);
        } else {
            return null;
        }
    }

    public synchronized Object peek() {
        if (!isEmpty()) {
            return list.get(0);
        } else {
            return null;
        }
    }

    public synchronized void clear() {
        list.clear();
    }

    public synchronized boolean isEmpty() {
        return list.isEmpty();
    }

    public synchronized int size() {
        return list.size();
    }

    public synchronized String toString() {
        return list.toString();
    }
}