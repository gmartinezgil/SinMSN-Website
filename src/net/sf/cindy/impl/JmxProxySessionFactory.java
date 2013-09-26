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

import javax.management.Attribute;
import javax.management.AttributeList;
import javax.management.AttributeNotFoundException;
import javax.management.ListenerNotFoundException;
import javax.management.MBeanAttributeInfo;
import javax.management.MBeanException;
import javax.management.MBeanInfo;
import javax.management.MBeanNotificationInfo;
import javax.management.NotCompliantMBeanException;
import javax.management.Notification;
import javax.management.NotificationBroadcasterSupport;
import javax.management.NotificationFilter;
import javax.management.NotificationListener;
import javax.management.ReflectionException;
import javax.management.StandardMBean;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeDataSupport;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenDataException;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;

import net.sf.cindy.Dispatcher;
import net.sf.cindy.EventGenerator;
import net.sf.cindy.Message;
import net.sf.cindy.MessageRecognizer;
import net.sf.cindy.Session;
import net.sf.cindy.SessionListener;
import net.sf.cindy.SessionStatistic;
import net.sf.cindy.spi.SessionSpi;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * Create session which support JMX. If current vm not support jmx, load this
 * class will throw NoClassDefFoundError.
 * 
 * @author Roger Chen
 */
public class JmxProxySessionFactory {

    private static final Log log = LogFactory
            .getLog(JmxProxySessionFactory.class);

    private static final String SESSION_ESTABLISHED = "cindy.sessionEstablished";
    private static final String SESSION_CLOSED = "cindy.sessionClosed";
    private static final String SESSION_TIMEOUT = "cindy.sessionTimeout";
    private static final String SESSION_IDLE = "cindy.sessionIdle";
    private static final String MESSAGE_RECEIVED = "cindy.messageReceived";
    private static final String MESSAGE_SENT = "cindy.messageSent";
    private static final String EXCEPTION_CAUGHT = "cindy.exceptionCaught";

    /**
     * Create jmx proxy session, the return session can be registered on
     * MBeanServer. If current vm not support jmx 1.2 specification, this 
     * method will return null.
     * 
     * @param session
     * 		the session 
     * @return
     * 		jmx proxy session
     */
    public static JmxProxySession createJmxProxySession(Session session) {
        if (!Constants.SUPPORT_JMX_1_2 || (!(session instanceof SessionSpi)))
            return null;
        try {
            return new JmxProxySessionImpl((SessionSpi) session);
        } catch (NotCompliantMBeanException e) {
            //Won't happend
            log.error(e, e);
            return null;
        }
    }

    /**
     * JmxProxySession implementation.
     * 
     * @author Roger Chen
     */
    private static class JmxProxySessionImpl extends StandardMBean implements
            JmxProxySession, SessionSpi, SessionMBean {

        private static final String[] ITEM_NAMES = new String[] {
                "ReceivedBytes", "SentBytes", "ElapsedTime", "AvgReceiveSpeed",
                "AvgSendSpeed", "ReceiveSpeed", "SendSpeed" };

        private static final String[] ITEM_DESCRIPTIONS = ITEM_NAMES;

        private static final OpenType[] ITEM_TYPES = new OpenType[] {
                SimpleType.LONG, SimpleType.LONG, SimpleType.LONG,
                SimpleType.DOUBLE, SimpleType.DOUBLE, SimpleType.DOUBLE,
                SimpleType.DOUBLE };

        private static final CompositeType SESSION_STATISTIC_COMPOSITE_TYPE;

        static {
            CompositeType type = null;
            try {
                type = new CompositeType("cindy.sessionStatistic",
                        "session statistic", ITEM_NAMES, ITEM_DESCRIPTIONS,
                        ITEM_TYPES);
            } catch (OpenDataException e) {
                log.error(e, e);
            }
            SESSION_STATISTIC_COMPOSITE_TYPE = type;
        }

        /**
         * Support emit notification.
         * 
         * @author Roger Chen
         */
        private class SessionNotificationBroadcaster extends
                NotificationBroadcasterSupport implements SessionListener {

            private long sequenceNumber = 1;

            private void sendNotification(Session session, String type,
                    String message) {
                sendNotification(new Notification(type,
                        JmxProxySessionImpl.this, sequenceNumber++, System
                                .currentTimeMillis(), message));
            }

            public void exceptionCaught(Session session, Throwable cause) {
                sendNotification(session, EXCEPTION_CAUGHT, cause.toString());
            }

            public void messageReceived(Session session, Message message) {
                sendNotification(session, MESSAGE_RECEIVED, message.toString());
            }

            public void messageSent(Session session, Message message) {
                sendNotification(session, MESSAGE_SENT, message.toString());
            }

            public void sessionClosed(Session session) {
                sendNotification(session, SESSION_CLOSED, "");
            }

            public void sessionEstablished(Session session) {
                sendNotification(session, SESSION_ESTABLISHED, "");
            }

            public void sessionIdle(Session session) {
                sendNotification(session, SESSION_IDLE, "");
            }

            public void sessionTimeout(Session session) {
                sendNotification(session, SESSION_TIMEOUT, "");
            }

        }

        private final SessionSpi session;
        private final SessionNotificationBroadcaster broadcaster = new SessionNotificationBroadcaster();

        public JmxProxySessionImpl(SessionSpi session)
                throws NotCompliantMBeanException {
            super(SessionMBean.class);
            this.session = session;
            session.addSessionListener(broadcaster);
        }

        public Session getSession() {
            return session;
        }

        public void removeNotificationListener(NotificationListener listener)
                throws ListenerNotFoundException {
            broadcaster.removeNotificationListener(listener);
        }

        public void removeNotificationListener(NotificationListener listener,
                NotificationFilter filter, Object handback)
                throws ListenerNotFoundException {
            broadcaster.removeNotificationListener(listener, filter, handback);
        }

        public void addNotificationListener(NotificationListener listener,
                NotificationFilter filter, Object handback)
                throws IllegalArgumentException {
            broadcaster.addNotificationListener(listener, filter, handback);
        }

        public MBeanNotificationInfo[] getNotificationInfo() {
            return broadcaster.getNotificationInfo();
        }

        protected synchronized void cacheMBeanInfo(MBeanInfo info) {
            //Custom MBeanInfo
            MBeanInfo beanInfo = null;
            if (info != null) {
                String[] types = new String[] { SESSION_ESTABLISHED,
                        SESSION_CLOSED, SESSION_IDLE, SESSION_TIMEOUT,
                        MESSAGE_RECEIVED, MESSAGE_SENT, EXCEPTION_CAUGHT };
                String name = Session.class.getName();
                String description = "Session notification";

                MBeanAttributeInfo[] attributes = info.getAttributes();

                for (int i = 0; i < attributes.length; i++) {
                    if (attributes[i].getName().equals("Statistic")) {
                        attributes[i] = new MBeanAttributeInfo(attributes[i]
                                .getName(), CompositeData.class.getName(),
                                attributes[i].getDescription(), attributes[i]
                                        .isReadable(), attributes[i]
                                        .isWritable(), attributes[i].isIs());
                    }
                }

                beanInfo = new MBeanInfo(
                        info.getClassName(),
                        info.getDescription(),
                        attributes,
                        info.getConstructors(),
                        info.getOperations(),
                        new MBeanNotificationInfo[] { new MBeanNotificationInfo(
                                types, name, description) });
            }
            super.cacheMBeanInfo(beanInfo);
        }

        public Object getAttribute(String attribute)
                throws AttributeNotFoundException, MBeanException,
                ReflectionException {
            if (attribute.equals("Statistic")) //Convert SessionStatistic to CompositeData
                return statisticToCompositeData();
            return super.getAttribute(attribute);
        }

        public AttributeList getAttributes(String[] attributes) {
            AttributeList list = super.getAttributes(attributes);
            for (int i = 0; i < list.size(); i++) {
                if (((Attribute) list.get(i)).getName().equals("Statistic")) { //Convert SessionStatistic to CompositeData
                    Attribute attr = new Attribute("Statistic",
                            statisticToCompositeData());
                    list.set(i, attr);
                }
            }
            return list;
        }

        /**
         * Convert SessionStatistic to CompositeData.
         * 
         * @return
         * 		CompositeData
         */
        private CompositeData statisticToCompositeData() {
            SessionStatistic stat = session.getStatistic();
            if (stat == null)
                return null;
            try {
                return new CompositeDataSupport(
                        SESSION_STATISTIC_COMPOSITE_TYPE, ITEM_NAMES,
                        new Object[] { Long.valueOf(stat.getReceivedBytes()),
                                Long.valueOf(stat.getSentBytes()),
                                Long.valueOf(stat.getElapsedTime()),
                                Double.valueOf(stat.getAvgReceiveSpeed()),
                                Double.valueOf(stat.getAvgSendSpeed()),
                                Double.valueOf(stat.getReceiveSpeed()),
                                Double.valueOf(stat.getSendSpeed()) });
            } catch (OpenDataException e) {
                //won't happen
                log.error(e, e);
                return null;
            }
        }

        public boolean getEnableStatistic() {
            return session.getEnableStatistic();
        }

        public void setEnableStatistic(boolean enable) {
            session.setEnableStatistic(enable);
        }

        public void setStatistic(SessionStatistic statistic) {
            session.setStatistic(statistic);
        }

        public void addSessionListener(SessionListener listener) {
            session.addSessionListener(listener);
        }

        public boolean blockWrite(Message message)
                throws IllegalArgumentException {
            return session.blockWrite(message);
        }

        public void close() {
            session.close();
        }

        public void close(boolean block) {
            session.close(block);
        }

        public void dispatchException(Throwable throwable) {
            session.dispatchException(throwable);
        }

        public Object getAttachment() {
            return session.getAttachment();
        }

        public int getBufferCapacityLimit() {
            return session.getBufferCapacityLimit();
        }

        public Dispatcher getDispatcher() {
            return session.getDispatcher();
        }

        public EventGenerator getEventGenerator() {
            return session.getEventGenerator();
        }

        public int getId() {
            return session.getId();
        }

        public MessageRecognizer getMessageRecognizer() {
            return session.getMessageRecognizer();
        }

        public int getSessionTimeout() {
            return session.getSessionTimeout();
        }

        public SessionStatistic getStatistic() {
            return session.getStatistic();
        }

        public int getWriteQueueSize() {
            return session.getWriteQueueSize();
        }

        public boolean isAvailable() {
            return session.isAvailable();
        }

        public boolean isClosing() {
            return session.isClosing();
        }

        public boolean isLogException() {
            return session.isLogException();
        }

        public boolean isStarted() {
            return session.isStarted();
        }

        public void onEvent(Object event, Object attachment) {
            session.onEvent(event, attachment);
        }

        public void removeSessionListener(SessionListener listener) {
            session.removeSessionListener(listener);
        }

        public void setAttachment(Object attachment) {
            session.setAttachment(attachment);
        }

        public void setBufferCapacityLimit(int bufferCapacityLimit) {
            session.setBufferCapacityLimit(bufferCapacityLimit);
        }

        public void setDispatcher(Dispatcher dispatcher) {
            session.setDispatcher(dispatcher);
        }

        public void setEventGenerator(EventGenerator generator) {
            session.setEventGenerator(generator);
        }

        public void setLogException(boolean logException) {
            session.setLogException(logException);
        }

        public void setMessageRecognizer(MessageRecognizer messageRecognizer) {
            session.setMessageRecognizer(messageRecognizer);
        }

        public void setSessionTimeout(int timeout) {
            session.setSessionTimeout(timeout);
        }

        public void start() throws IllegalStateException {
            session.start();
        }

        public void start(boolean block) throws IllegalStateException {
            session.start(block);
        }

        public void write(Message message) throws IllegalArgumentException {
            session.write(message);
        }
    }

}