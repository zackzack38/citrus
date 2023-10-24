/*
 *  Copyright 2023 the original author or authors.
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements. See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.consol.citrus.message;

import java.util.List;

import com.consol.citrus.DefaultTestCaseRunner;
import com.consol.citrus.TestCaseRunner;
import com.consol.citrus.context.TestContext;
import com.consol.citrus.context.TestContextFactory;
import com.consol.citrus.endpoint.direct.DirectEndpoint;
import com.consol.citrus.endpoint.direct.DirectEndpointBuilder;
import com.consol.citrus.exceptions.ValidationException;
import com.consol.citrus.validation.MessageValidator;
import com.consol.citrus.validation.context.ValidationContext;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static com.consol.citrus.actions.ReceiveMessageAction.Builder.receive;
import static com.consol.citrus.actions.SendMessageAction.Builder.send;

public class MessageStoreTest {

    private final DirectEndpoint directEndpoint = new DirectEndpointBuilder()
            .queue(new DefaultMessageQueue("foo"))
            .build();

    private TestContext context;
    private TestCaseRunner t;

    @BeforeMethod
    public void createTestContext() {
        context = TestContextFactory.newInstance().getObject();
        t = new DefaultTestCaseRunner(context);

        context.getMessageValidatorRegistry().addMessageValidator("simple", new MessageValidator<>() {
            @Override
            public void validateMessage(Message receivedMessage, Message controlMessage, TestContext context, List<ValidationContext> validationContexts) throws ValidationException {
                Assert.assertEquals(receivedMessage.getPayload(), controlMessage.getPayload());
            }

            @Override
            public boolean supportsMessageType(String messageType, Message message) {
                return true;
            }
        });
    }

    @Test
    public void shouldStoreMessages() {
        t.run(send()
                .endpoint(directEndpoint)
                .message()
                .name("request")
                .body("Citrus rocks!"));

        Assert.assertNotNull(context.getMessageStore().getMessage("request"));
        Assert.assertEquals(context.getMessageStore().getMessage("request").getPayload(String.class), "Citrus rocks!");

        t.run(receive()
                .endpoint(directEndpoint)
                .message()
                .name("response")
                .body("Citrus rocks!"));

        Assert.assertNotNull(context.getMessageStore().getMessage("response"));
        Assert.assertEquals(context.getMessageStore().getMessage("response").getPayload(String.class), "Citrus rocks!");
    }

    @Test
    public void shouldStoreMessagesFromValidationCallback() {
        t.run(send()
                .endpoint(directEndpoint)
                .message()
                .name("request")
                .body("Citrus is awesome!"));

        Assert.assertNotNull(context.getMessageStore().getMessage("request"));
        Assert.assertEquals(context.getMessageStore().getMessage("request").getPayload(String.class), "Citrus is awesome!");

        t.run(receive()
                .endpoint(directEndpoint)
                .message()
                .name("response")
                .validate((message, context) -> {
                    Message request = context.getMessageStore().getMessage("request");
                    Assert.assertEquals(message.getPayload(), request.getPayload());
                }));

        Assert.assertNotNull(context.getMessageStore().getMessage("response"));
        Assert.assertEquals(context.getMessageStore().getMessage("response").getPayload(String.class), "Citrus is awesome!");
    }
}
