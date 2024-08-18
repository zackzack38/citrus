package org.citrusframework.openapi.generator;

import org.citrusframework.annotations.CitrusResource;
import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.config.CitrusSpringConfig;
import org.citrusframework.context.TestContext;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpEndpointConfiguration;
import org.citrusframework.junit.jupiter.spring.CitrusSpringSupport;
import org.citrusframework.openapi.generator.SpringBeanConfigurationIT.ClientConfiguration;
import org.citrusframework.openapi.generator.rest.petstore.spring.PetStoreBeanConfiguration;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;

@CitrusSpringSupport
@ContextConfiguration(classes = {CitrusSpringConfig.class, ClientConfiguration.class, PetStoreBeanConfiguration.class})
class SpringBeanConfigurationIT {

    @Autowired
    private ApplicationContext applicationContext;

    @Test
    @CitrusTest
    void fromReferenceResolverIsPrototypeScoped(@CitrusResource TestContext testContext) {
// TODO: the intention is to provide "requests" by the generated openApi class. Thus, no action
//  beans need to be present in the context. Remove this test and provide other test, that proves,
//  that the API is present.
        //        var addPetRequest = testContext.getReferenceResolver().resolve(AddPetRequest.class);
//        assertThat(addPetRequest)
//            .isNotNull()
//            .isNotEqualTo(testContext.getReferenceResolver().resolve(AddPetRequest.class));
    }

    @Test
    void fromSpringApplicationContextIsPrototypeScoped() {
// TODO: the intention is to provide "requests" by the generated openApi class. Thus, no action
//  beans need to be present in the context. Remove this test and provide other test, that proves,
//  that the API is present.
//        assertThat(applicationContext.getBean(AddPetRequest.class))
//            .isNotNull()
//            .isNotEqualTo(applicationContext.getBean(AddPetRequest.class));
    }

    @TestConfiguration
    public static class ClientConfiguration {

        @Bean(name= {"applicationServiceClient", "petStoreEndpoint"})
        public HttpClient applicationServiceClient() {
            var config = new HttpEndpointConfiguration();
            config.setRequestUrl("http://localhost:9000");
            return new HttpClient(config);
        }
    }
}
