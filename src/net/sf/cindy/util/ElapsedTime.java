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
package net.sf.cindy.util;

/**
 * Elapsed time. Adjust system time will cause System.currentTimeMillis()
 * backward.
 * 
 * @author Roger Chen
 */
public final class ElapsedTime {

    private static final boolean SUPPORT_NANO_TIME = !Utils.isJdk14();

    private long startTime;

    public ElapsedTime() {
        if (SUPPORT_NANO_TIME)
            startTime = System.nanoTime();
        else
            startTime = System.currentTimeMillis();
    }

    /**
     * Get elapsed time in milliseconds.
     * 
     * @return
     * 		elapsed time
     */
    public long getElapsedTime() {
        if (SUPPORT_NANO_TIME)
            return (long) ((System.nanoTime() - startTime) / 1e6);
        else {
            long currentTime = System.currentTimeMillis();
            if (currentTime > startTime)
                return currentTime - startTime;
            return 0;
        }
    }

    /**
     * Reset start time.
     * 
     * @return
     * 		elapsed time
     */
    public long reset() {
        long currentTime;
        long elapsedTime;
        if (SUPPORT_NANO_TIME) {
            currentTime = System.nanoTime();
            elapsedTime = (long) ((currentTime - startTime) / 1e6);
        } else {
            currentTime = System.currentTimeMillis();
            elapsedTime = (currentTime > startTime) ? currentTime - startTime
                    : 0;
        }
        startTime = currentTime;
        return elapsedTime;
    }
}