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

import net.sf.cindy.util.Utils;

/**
 * Some constants, can be changed by init system properties before.
 * 
 * @author Roger Chen
 */
class Constants {

    /**
     * Register a session in {@link net.sf.cindy.EventGenerator EventGenerator}.
     */
    static final Object EV_REGISTER = new String("register");

    /**
     * Unregister a session in {@link net.sf.cindy.EventGenerator EventGenerator}.
     */
    static final Object EV_UNREGISTER = new String("unregister");

    /**
     * Enable a session to read in {@link net.sf.cindy.EventGenerator EventGenerator}.
     */
    static final Object EV_ENABLE_READ = new String("enable read");

    /**
     * Enable a session to write in {@link net.sf.cindy.EventGenerator EventGenerator}.
     */
    static final Object EV_ENABLE_WRITE = new String("enable write");

    /**
     * The session is acceptable for incoming session.
     */
    static final Object EV_ACCEPTABLE = new String("acceptable");

    /**
     * The session is connectable.
     */
    static final Object EV_CONNECTABLE = new String("connectable");

    /**
     * The session is readable.
     */
    static final Object EV_READABLE = new String("readable");

    /**
     * The session is writable.
     */
    static final Object EV_WRITABLE = new String("writable");

    /**
     * Check the session is timeout.
     */
    static final Object EV_CHECK_TIMEOUT = new String("check timeout");

    /**
     * Some event happen, set the session timeout to 0.
     */
    static final Object EV_EVENT_HAPPEN = new String("event happen");

    private static final String KEY_SESSION_TIMEOUT = "net.sf.cindy.sessionTimeout";
    private static final String KEY_BUFFER_CAPACITY = "net.sf.cindy.bufferCapacity";
    private static final String KEY_BUFFER_CAPACITY_LIMIT = "net.sf.cindy.bufferCapacityLimit";
    private static final String KEY_USE_DIRECT_BUFFER = "net.sf.cindy.useDirectBuffer";
    private static final String KEY_CHECK_SESSION_TIMEOUT_INTERVAL = "net.sf.cindy.checkSessionInterval";
    private static final String KEY_LOG_EXCEPTION = "net.sf.cindy.logException";

    private static final int DEF_SESSION_TIMEOUT = 0;
    private static final int DEF_BUFFER_CAPACITY = 8 * 1024;
    private static final int DEF_BUFFER_CAPACITY_LIMIT = 128 * 1024;
    private static final boolean DEF_USE_DIRECT_BUFFER = false;
    private static final int DEF_CHECK_SESSION_TIMEOUT_INTERVAL = 1000;
    private static final boolean DEF_LOG_EXCEPTION = false;

    /**
     * Session default timeout.
     */
    static final int SESSION_TIMEOUT;

    /**
     * Session default buffer capacity.
     */
    static final int BUFFER_CAPACITY;

    /**
     * Session default buffer capacity limit.
     */
    static final int BUFFER_CAPACITY_LIMIT;

    /**
     * Session use DirectByteBuffer or ByteBuffer.
     */
    static final boolean USE_DIRECT_BUFFER;

    /**
     * Check session timeout interval.
     */
    static final int CHECK_SESSION_TIMEOUT_INTERVAL;

    /**
     * Session default log exception.
     */
    static final boolean LOG_EXCEPTION;

    /**
     * In JDK 1.4, it's has a bug when invoke SocketChannel.write(ByteBuffer[]),
     * see: http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=4854354.
     * <p>
     * So when run on 1.4, It's only invoke SocketChannel.write(ByteBuffer).
     */
    static final boolean SUPPORT_WRITE_BUFFER_ARRAY;

    /**
     * Class SSLEngine was introduced in JDK 1.5
     */
    static final boolean SUPPORT_SSL;

    /**
     * Support jmx 1.2 specification.
     */
    static final boolean SUPPORT_JMX_1_2;

    static {
        int bufferCapacity = Integer.getInteger(KEY_BUFFER_CAPACITY,
                DEF_BUFFER_CAPACITY).intValue();
        if (bufferCapacity <= 0)
            bufferCapacity = DEF_BUFFER_CAPACITY;
        BUFFER_CAPACITY = bufferCapacity;

        int bufferCapacityLimit = Integer.getInteger(KEY_BUFFER_CAPACITY_LIMIT,
                DEF_BUFFER_CAPACITY_LIMIT).intValue();
        if (bufferCapacityLimit < bufferCapacity)
            bufferCapacityLimit = bufferCapacity;
        BUFFER_CAPACITY_LIMIT = bufferCapacityLimit;

        Integer isUseDirectBuffer = Integer.getInteger(KEY_USE_DIRECT_BUFFER);
        if (isUseDirectBuffer == null)
            USE_DIRECT_BUFFER = DEF_USE_DIRECT_BUFFER;
        else
            USE_DIRECT_BUFFER = isUseDirectBuffer.intValue() != 0;

        int checkInterval = Integer.getInteger(
                KEY_CHECK_SESSION_TIMEOUT_INTERVAL,
                DEF_CHECK_SESSION_TIMEOUT_INTERVAL).intValue();
        if (checkInterval < 0)
            checkInterval = DEF_CHECK_SESSION_TIMEOUT_INTERVAL;
        CHECK_SESSION_TIMEOUT_INTERVAL = checkInterval;

        int sessionTimeout = Integer.getInteger(KEY_SESSION_TIMEOUT,
                DEF_SESSION_TIMEOUT).intValue();
        if (sessionTimeout < 0)
            sessionTimeout = 0;
        SESSION_TIMEOUT = sessionTimeout;

        Integer logException = Integer.getInteger(KEY_LOG_EXCEPTION);
        if (logException == null)
            LOG_EXCEPTION = DEF_LOG_EXCEPTION;
        else
            LOG_EXCEPTION = logException.intValue() != 0;

        SUPPORT_WRITE_BUFFER_ARRAY = !Utils.isJdk14();
        SUPPORT_SSL = SUPPORT_WRITE_BUFFER_ARRAY;

        SUPPORT_JMX_1_2 = Utils.isSupportJmx12();
    }

    private Constants() {
    }

}