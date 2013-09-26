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
 * Compute speed.
 * 
 * @author Roger Chen
 */
public final class Speed {

    private final ElapsedTime time = new ElapsedTime();

    private final int refreshRate;

    /*
     * instantaneous speed = values[x] * rate[0] + values[x+1] * rate[1] + ... / len
     * 
     * rate[0] + rate[1] + ... + rate[len-1] = n
     * 
     * rate[i] > 0
     */
    private final long values[];
    private final double rate[];

    private long lastUpdateTimeFactor;
    private long currentValue;
    private long totalValue;

    private Speed(int refreshRate, int n) {
        this.refreshRate = refreshRate;
        this.values = new long[n];

        this.rate = new double[n];
        /* 
         * rate[0] = na , rate[1] = (n-1)a , ... , rate[n-1] = a
         * (1 + ... + n) * a = n
         * n * (n + 1) * a / 2 = n
         */
        double a = (double) 2 / (n + 1);
        for (int i = 0; i < rate.length; i++) {
            rate[i] = (rate.length - i) * a;
        }
    }

    /**
     * create an instance.
     * 
     * @param refreshRate
     * 		refresh rate in ms
     * @param period
     * 		period in ms
     * @return
     * 		speed instance
     */
    public static Speed getInstance(int refreshRate, int period) {
        if (period < refreshRate)
            return null;
        return new Speed(refreshRate, period / refreshRate);
    }

    /**
     * create an instance by default parameter.
     * 
     * @return
     * 		speed instance
     */
    public static Speed getInstance() {
        return getInstance(1000, 5000);
    }

    /**
     * Reset all values.
     */
    public synchronized void reset() {
        lastUpdateTimeFactor = 0;
        currentValue = 0;
        totalValue = 0;
        for (int i = 0; i < values.length; i++) {
            values[i] = 0;
        }
        time.reset();
    }

    private int getIndex(long factor) {
        return Math.abs((int) (factor % values.length));
    }

    private synchronized void update(long timeFactor) {
        if (lastUpdateTimeFactor == timeFactor)
            return;

        values[getIndex(lastUpdateTimeFactor)] = currentValue;
        currentValue = 0;

        if (lastUpdateTimeFactor < timeFactor - values.length)
            //Reduce Loop
            lastUpdateTimeFactor = timeFactor - values.length - 1;
        //Reset old values
        for (long i = lastUpdateTimeFactor + 1; i < timeFactor; i++) {
            values[getIndex(i)] = 0;
        }
        lastUpdateTimeFactor = timeFactor;
    }

    /**
     * Add value, such as received bytes(unit).
     * 
     * @param value
     * 		value
     */
    public void addValue(long value) {
        long timeFactor = time.getElapsedTime() / refreshRate;
        update(timeFactor);
        currentValue += value;
        totalValue += value;
    }

    /**
     * Get total value, such as total received bytes(unit).
     * 
     * @return
     * 		total value
     */
    public long getTotalValue() {
        return totalValue;
    }

    /**
     * Get elapsed time(ms).
     * 
     * @return
     * 		elapsed time
     */
    public long getElapsedTime() {
        return time.getElapsedTime();
    }

    /**
     * Get average speed(unit/s). 
     * 
     * @return
     * 		average speed
     */
    public double getAvgSpeed() {
        long elapsedTime = time.getElapsedTime();
        if (totalValue == 0 && elapsedTime == 0)
            return 0;
        return (double) totalValue * 1000 / elapsedTime;
    }

    /**
     * Get current speed(unit/s). 
     * 
     * @return
     * 		current speed
     */
    public double getSpeed() {
        long timeFactor = time.getElapsedTime() / refreshRate;
        update(timeFactor);

        double sum = 0;
        for (int i = 0; i < values.length; i++) {
            int index = getIndex(timeFactor - i - 1);
            sum += values[index] * rate[i];
        }
        return sum * 1000 / refreshRate / values.length;
    }
}