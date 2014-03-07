/*
 * Copyright 2013 Vaadin Ltd.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.vaadin.tori.util;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.portlet.PortletRequest;

import org.apache.log4j.Logger;
import org.vaadin.tori.PortletRequestAware;

import com.liferay.portal.kernel.messaging.Destination;
import com.liferay.portal.kernel.messaging.Message;
import com.liferay.portal.kernel.messaging.MessageBusUtil;
import com.liferay.portal.kernel.messaging.MessageListener;
import com.liferay.portal.kernel.messaging.ParallelDestination;

public class LiferayCommonToriActivityMessaging implements
        ToriActivityMessaging, PortletRequestAware {

    private static final String USER_TYPING_DESTINATION = "tori/activity/usertyping";
    private static final String USER_AUTHORED_DESTINATION = "tori/activity/userauthored";

    private static final String SENDER_ID = "SENDER_ID";

    private static final String USER_ID = "USER_ID";
    private static final String THREAD_ID = "THREAD_ID";
    private static final String STARTED_TYPING = "STARTED_TYPING";
    private static final String POST_ID = "POST_ID";

    private PortletRequest request;
    private long currentUserId;

    private final Map<Object, MessageListener> listeners = new HashMap<Object, MessageListener>();

    private final Logger log = Logger
            .getLogger(LiferayCommonToriActivityMessaging.class);

    public LiferayCommonToriActivityMessaging() {
        for (String destinationName : Arrays.asList(USER_AUTHORED_DESTINATION,
                USER_TYPING_DESTINATION)) {
            if (!MessageBusUtil.getMessageBus().hasDestination(destinationName)) {
                log.info("Adding a message bus destination: " + destinationName);
                @SuppressWarnings("deprecation")
                Destination destination = new ParallelDestination(
                        destinationName);
                destination.open();
                MessageBusUtil.addDestination(destination);
            }
        }
    }

    @Override
    public void sendUserTyping(final long threadId, final Date startedTyping) {
        Message message = new Message();
        message.put(USER_ID, new Long(currentUserId));
        message.put(THREAD_ID, new Long(threadId));
        message.put(STARTED_TYPING, startedTyping.getTime());
        sendMessage(message, USER_TYPING_DESTINATION);
    }

    @Override
    public void sendUserAuthored(final long postId, final long threadId) {
        Message message = new Message();
        message.put(POST_ID, new Long(postId));
        message.put(THREAD_ID, new Long(threadId));
        sendMessage(message, USER_AUTHORED_DESTINATION);
    }

    private void sendMessage(final Message message, final String destinationName) {
        message.put(SENDER_ID, getSenderId());
        MessageBusUtil.sendMessage(destinationName, message);
    }

    private String getSenderId() {
        return request.getPortletSession().getId();
    }

    private boolean isThisSender(final Message message) {
        Object senderId = message.get(SENDER_ID);
        return senderId != null && senderId.equals(getSenderId());
    }

    @Override
    public void addUserTypingListener(final UserTypingListener listener) {
        addListener(listener, new FilteringMessageListener(
                USER_TYPING_DESTINATION) {
            @Override
            protected void process(final Message message) {
                listener.userTyping(message.getLong(USER_ID),
                        message.getLong(THREAD_ID),
                        new Date(message.getLong(STARTED_TYPING)));
            }
        });
    }

    @Override
    public void addUserAuthoredListener(final UserAuthoredListener listener) {
        addListener(listener, new FilteringMessageListener(
                USER_AUTHORED_DESTINATION) {
            @Override
            protected void process(final Message message) {
                listener.userAuthored(message.getLong(POST_ID),
                        message.getLong(THREAD_ID));
            }
        });
    }

    @Override
    public void removeUserTypingListener(final UserTypingListener listener) {
        removeListener(listener, USER_TYPING_DESTINATION);
    }

    @Override
    public void removeUserAuthoredListener(final UserAuthoredListener listener) {
        removeListener(listener, USER_AUTHORED_DESTINATION);
    }

    private void addListener(final Object key,
            final FilteringMessageListener messageListener) {
        MessageBusUtil.registerMessageListener(messageListener.destinationName,
                messageListener);
        listeners.put(key, messageListener);
    }

    private boolean isSessionAlive() {
        boolean result = true;
        try {
            request.getPortletSession().getAttribute("test");
        } catch (IllegalStateException e) {
            result = false;
        }
        return result;
    }

    private void removeListener(final Object key, final String destination) {
        try {
            MessageListener messageListener = listeners.remove(key);
            if (messageListener != null) {
                MessageBusUtil.unregisterMessageListener(destination,
                        messageListener);
            }
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void setRequest(final PortletRequest request) {
        this.request = request;
        if (currentUserId == 0 && request.getRemoteUser() != null) {
            currentUserId = Long.valueOf(request.getRemoteUser());
        }
    }

    private abstract class FilteringMessageListener implements MessageListener {

        private final String destinationName;

        public FilteringMessageListener(final String destinationName) {
            this.destinationName = destinationName;
        }

        @Override
        public void receive(final Message message) {
            if (isSessionAlive()) {
                if (!isThisSender(message)) {
                    try {
                        process(message);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            } else {
                MessageBusUtil.unregisterMessageListener(destinationName, this);
            }
        }

        protected abstract void process(Message message);

    }

}
