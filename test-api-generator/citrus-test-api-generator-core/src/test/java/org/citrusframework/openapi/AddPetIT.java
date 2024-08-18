package org.citrusframework.openapi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.citrusframework.util.FileUtils.readToString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.networknt.schema.JsonSchema;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;
import org.citrusframework.Citrus;
import org.citrusframework.CitrusInstanceManager;
import org.citrusframework.DefaultTestCaseRunner;
import org.citrusframework.TestAction;
import org.citrusframework.TestActor;
import org.citrusframework.TestCase;
import org.citrusframework.actions.SendMessageAction.SendMessageActionBuilder;
import org.citrusframework.annotations.CitrusAnnotations;
import org.citrusframework.common.SpringXmlTestLoader;
import org.citrusframework.common.TestLoader;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.context.TestContext;
import org.citrusframework.endpoint.EndpointConfiguration;
import org.citrusframework.endpoint.direct.DirectEndpoint;
import org.citrusframework.endpoint.direct.DirectEndpointBuilder;
import org.citrusframework.exceptions.ValidationException;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpEndpointConfiguration;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.http.server.HttpServerBuilder;
import org.citrusframework.json.schema.SimpleJsonSchema;
import org.citrusframework.junit.jupiter.spring.CitrusSpringExtension;
import org.citrusframework.message.DefaultMessage;
import org.citrusframework.message.DefaultMessageQueue;
import org.citrusframework.message.Message;
import org.citrusframework.message.MessageQueue;
import org.citrusframework.messaging.Producer;
import org.citrusframework.messaging.SelectiveConsumer;
import org.citrusframework.openapi.AddPetIT.Config;
import org.citrusframework.openapi.generator.rest.petstore.spring.PetStoreBeanConfiguration;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.spi.Resources;
import org.citrusframework.testapi.ApiActionBuilderCustomizerService;
import org.citrusframework.testapi.GeneratedApi;
import org.citrusframework.util.SocketUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatcher;
import org.mockito.ArgumentMatchers;
import org.mockito.Mock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;

@ExtendWith(CitrusSpringExtension.class)
@SpringBootTest(classes = {PetStoreBeanConfiguration.class, CitrusSpringConfig.class, Config.class})
class AddPetIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Autowired
    private MessageQueue messageQueue;

    private TestContext testContext;

    @BeforeEach
    void beforeEach() {
        testContext = applicationContext.getBean(TestContext.class);
    }

    @Test
    void testValidationFailure() {
        executeTest("addPetRequestTest", testContext);
        System.out.println("asdf");
    }

    private TestCase executeTest(String testName, TestContext testContext) {
        assertThat(CitrusInstanceManager.get()).isPresent();

        Citrus citrus = CitrusInstanceManager.get().get();
        TestLoader loader = new SpringXmlTestLoader().citrusContext(citrus.getCitrusContext())
            .citrus(citrus)
            .context(testContext);
        loader.setTestName(testName);
        loader.setPackageName("org.citrusframework.openapi.generator.GeneratedApiTest");
        loader.load();
        return loader.getTestCase();
    }

    public static class Config {

        @Bean
        public HttpServer httpServer() {
            int port = SocketUtils.findAvailableTcpPort(8080);
            return new HttpServerBuilder()
                .port(port)
                .timeout(5000L)
                .autoStart(true)
                .defaultStatus(HttpStatus.NO_CONTENT)
                .build();
        }

//        @Bean(name = {"applicationServiceClient", "multipartTestEndpoint",
//            "soapSampleStoreEndpoint", "petStoreEndpoint"})
//        public HttpClient applicationServiceClient() {
//            HttpClient clientMock = mock();
//            EndpointConfiguration endpointConfigurationMock = mock();
//            when(clientMock.getEndpointConfiguration()).thenReturn(new HttpEndpointConfiguration());
//            when(endpointConfigurationMock.getTimeout()).thenReturn(5000L);
//            return clientMock;
//        }

        @Bean(name = {"applicationServiceClient", "multipartTestEndpoint",
            "soapSampleStoreEndpoint", "petStoreEndpoint"})
        public DirectEndpoint applicationServiceClient(MessageQueue testQueue) {
            return new DirectEndpointBuilder()
                .queue(testQueue)
                .build();
        }

        @Bean(name = "test.queue")
        public MessageQueue testQueue() {
            return new DefaultMessageQueue("test.queue");
        }

        @Bean
        public ApiActionBuilderCustomizerService customizer() {
            return new ApiActionBuilderCustomizerService() {
                @Override
                public <T extends SendMessageActionBuilder<?, ?, ?>> T build(
                    GeneratedApi generatedApi, TestAction action, TestContext context, T builder) {
                    builder.getMessageBuilderSupport()
                        .header("x-citrus-api-version", generatedApi.getApiVersion());
                    return builder;
                }
            };
        }

        @Bean({"oas3", "testSchema"})
        public SimpleJsonSchema testSchema() {
            JsonSchema schemaMock = mock();
            SimpleJsonSchema jsonSchemaMock = mock();

            when(jsonSchemaMock.getSchema()).thenReturn(schemaMock);

            Set<ValidationMessage> okReport = new HashSet<>();
            when(schemaMock.validate(any())).thenReturn(okReport);
            return jsonSchemaMock;
        }

        @Bean
        public SimpleJsonSchema failingTestSchema() {
            JsonSchema schemaMock = mock();
            SimpleJsonSchema jsonSchemaMock = mock();

            when(jsonSchemaMock.getSchema()).thenReturn(schemaMock);

            Set<ValidationMessage> nokReport = new HashSet<>();
            nokReport.add(new ValidationMessage.Builder().customMessage(
                "This is a simulated validation error message").build());
            when(schemaMock.validate(any())).thenReturn(nokReport);
            return jsonSchemaMock;
        }

        @Bean
        public TestActor testActor() {
            return new TestActor();
        }
    }
}
