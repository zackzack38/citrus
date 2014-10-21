/*
 * Copyright 2006-2013 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.consol.citrus.jms.endpoint;

import com.consol.citrus.context.TestContext;
import com.consol.citrus.exceptions.ActionTimeoutException;
import com.consol.citrus.messaging.AbstractSelectiveMessageConsumer;
import com.consol.citrus.message.Message;
import com.consol.citrus.report.MessageListeners;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;

/**
 * @author Christoph Deppisch
 * @since 1.4
 */
public class JmsConsumer extends AbstractSelectiveMessageConsumer {

    /** Logger */
    private static Logger log = LoggerFactory.getLogger(JmsConsumer.class);

    /** Endpoint configuration */
    private final JmsEndpointConfiguration endpointConfiguration;

    /** Message listener  */
    private final MessageListeners messageListener;

    /**
     * Default constructor using endpoint.
     * @param endpointConfiguration
     * @param messageListener
     */
    public JmsConsumer(JmsEndpointConfiguration endpointConfiguration, MessageListeners messageListener) {
        super(endpointConfiguration);
        this.endpointConfiguration = endpointConfiguration;
        this.messageListener = messageListener;
    }

    @Override
    public Message receive(String selector, TestContext context, long timeout) {
        String destinationName;

        if (StringUtils.hasText(selector)) {
            destinationName = endpointConfiguration.getDefaultDestinationName() + "(" + selector + ")'";
        } else {
            destinationName = endpointConfiguration.getDefaultDestinationName();
        }

        log.info("Waiting for JMS message on destination: '" + destinationName + "'");

        endpointConfiguration.getJmsTemplate().setReceiveTimeout(timeout);
        javax.jms.Message receivedJmsMessage;

        if (StringUtils.hasText(selector)) {
            receivedJmsMessage = endpointConfiguration.getJmsTemplate().receiveSelected(selector);
        } else {
            receivedJmsMessage = endpointConfiguration.getJmsTemplate().receive();
        }

        if (receivedJmsMessage == null) {
            throw new ActionTimeoutException("Action timed out while receiving JMS message on '" + destinationName + "'");
        }

        Message receivedMessage = endpointConfiguration.getMessageConverter().convertInbound(receivedJmsMessage, endpointConfiguration);

        log.info("Received JMS message on destination: '" + destinationName + "'");
        onInboundMessage(receivedMessage);

        return receivedMessage;
    }

    /**
     * Informs message listeners if present.
     * @param receivedMessage
     */
    protected void onInboundMessage(Message receivedMessage) {
        if (messageListener != null) {
            messageListener.onInboundMessage(receivedMessage);
        } else {
            log.debug("Received message is:" + System.getProperty("line.separator") + (receivedMessage != null ? receivedMessage.toString() : ""));
        }
    }

    /**
     * Gets the message listener.
     * @return
     */
    public MessageListeners getMessageListener() {
        return messageListener;
    }
}
