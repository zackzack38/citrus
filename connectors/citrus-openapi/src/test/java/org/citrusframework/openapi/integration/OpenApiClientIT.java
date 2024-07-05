/*
 * Copyright the original author or authors.
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

package org.citrusframework.openapi.integration;

import static org.citrusframework.http.actions.HttpActionBuilder.http;
import static org.citrusframework.openapi.actions.OpenApiActionBuilder.openapi;
import static org.testng.Assert.assertThrows;
import static org.testng.Assert.fail;

import org.citrusframework.annotations.CitrusTest;
import org.citrusframework.exceptions.TestCaseFailedException;
import org.citrusframework.http.actions.HttpClientRequestActionBuilder.HttpMessageBuilderSupport;
import org.citrusframework.http.client.HttpClient;
import org.citrusframework.http.client.HttpClientBuilder;
import org.citrusframework.http.server.HttpServer;
import org.citrusframework.http.server.HttpServerBuilder;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.actions.OpenApiActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientResponseActionBuilder;
import org.citrusframework.spi.BindToRegistry;
import org.citrusframework.spi.Resources;
import org.citrusframework.testng.spring.TestNGCitrusSpringSupport;
import org.citrusframework.util.SocketUtils;
import org.springframework.http.HttpStatus;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Ignore;
import org.testng.annotations.Test;

/**
 * @author Christoph Deppisch
 */
public class OpenApiClientIT extends TestNGCitrusSpringSupport {

    public static final String VALID_PET_PATH = "classpath:org/citrusframework/openapi/petstore/pet.json";
    public static final String INVALID_PET_PATH = "classpath:org/citrusframework/openapi/petstore/pet_invalid.json";

    private final int port = SocketUtils.findAvailableTcpPort(8080);

    @BindToRegistry
    private final HttpServer httpServer = new HttpServerBuilder()
            .port(port)
            .timeout(5000L)
            .autoStart(true)
            .defaultStatus(HttpStatus.NO_CONTENT)
            .build();

    @BindToRegistry
    private final HttpClient httpClient = new HttpClientBuilder()
            .requestUrl("http://localhost:%d".formatted(port))
            .build();

    private final OpenApiSpecification petstoreSpec = OpenApiSpecification.from(
            Resources.create("classpath:org/citrusframework/openapi/petstore/petstore-v3.json"));

    private final OpenApiSpecification pingSpec = OpenApiSpecification.from(
        Resources.create("classpath:org/citrusframework/openapi/ping/ping-api.yaml"));

    @CitrusTest
    @Test
    public void shouldExecuteGetPetByIdFromDirectSpec() {
        shouldExecuteGetPetById(openapi(petstoreSpec), VALID_PET_PATH, true, false);
    }

    @CitrusTest
    @Test
    public void shouldFailOnMissingNameInResponse() {
        shouldExecuteGetPetById(openapi(petstoreSpec), INVALID_PET_PATH, false, false);
    }

    @CitrusTest
    @Test
    public void shouldSucceedOnMissingNameInResponseWithValidationDisabled() {
        shouldExecuteGetPetById(openapi(petstoreSpec), INVALID_PET_PATH, true, true);
    }

    private void shouldExecuteGetPetById(OpenApiActionBuilder openapi, String responseFile,
        boolean valid, boolean disableValidation) {

        variable("petId", "1001");

        when(openapi
                .client(httpClient)
                .send("getPetById")
                .message()
                .fork(true));

        then(http().server(httpServer)
                .receive()
                .get("/pet/${petId}")
                .message()
                .accept("@contains('application/json')@"));

        then(http().server(httpServer)
                .send()
                .response(HttpStatus.OK)
                .message()
                .body(Resources.create(responseFile))
                .contentType("application/json"));

        OpenApiClientResponseActionBuilder clientResponseActionBuilder = openapi
            .client(httpClient).receive("getPetById", HttpStatus.OK)
            .disableOasValidation(disableValidation);

        if (valid) {
            then(clientResponseActionBuilder);
        } else {
            assertThrows(() -> then(clientResponseActionBuilder));
        }
    }

    @CitrusTest
    @Test
    public void shouldProperlyExecuteGetAndAddPetFromDirectSpec() {
        shouldExecuteGetAndAddPet(openapi(petstoreSpec));
    }

    @CitrusTest
    @Test
    public void shouldProperlyExecuteGetAndAddPetFromRepository() {
        shouldExecuteGetAndAddPet(openapi(petstoreSpec));
    }

    @CitrusTest
    @Test
    public void shouldFailOnMissingNameInRequest() {
        variable("petId", "1001");

        HttpMessageBuilderSupport addPetBuilder = openapi(petstoreSpec)
            .client(httpClient)
            .send("addPet")
            .message().body(Resources.create(INVALID_PET_PATH))
            .fork(true);

        assertThrows(TestCaseFailedException.class, () ->when(addPetBuilder));
    }

    @CitrusTest
    @Test
    public void shouldFailOnWrongQueryIdType() {
        variable("petId", "xxxx");
        HttpMessageBuilderSupport addPetBuilder = openapi(petstoreSpec)
            .client(httpClient)
            .send("addPet")
            .message().body(Resources.create(VALID_PET_PATH))
            .fork(true);

        assertThrows(TestCaseFailedException.class, () ->when(addPetBuilder));
    }

    @CitrusTest
    @Test
    public void shouldSucceedOnWrongQueryIdTypeWithOasDisabled() {
        variable("petId", "xxxx");
        HttpMessageBuilderSupport addPetBuilder = openapi(petstoreSpec)
            .client(httpClient)
            .send("addPet")
            .disableOasValidation(true)
            .message().body(Resources.create(VALID_PET_PATH))
            .fork(true);

        try {
            when(addPetBuilder);
        } catch (Exception e) {
            fail("Method threw an exception: " + e.getMessage());
        }

    }

    private void shouldExecuteGetAndAddPet(OpenApiActionBuilder openapi) {

        variable("petId", "1001");

        when(openapi
                .client(httpClient)
                .send("addPet")
                .fork(true));

        then(http().server(httpServer)
                .receive()
                .post("/pet")
                .message()
                .body("""
                        {
                          "id": "@isNumber()@",
                          "name": "@notEmpty()@",
                          "category": {
                            "id": "@isNumber()@",
                            "name": "@notEmpty()@"
                          },
                          "photoUrls": "@notEmpty()@",
                          "tags":  "@ignore@",
                          "status": "@matches(sold|pending|available)@"
                        }
                """)
                .contentType("application/json;charset=UTF-8"));

        then(http().server(httpServer)
                .send()
                .response(HttpStatus.CREATED)
                .message());

        then(openapi
                .client(httpClient)
                .receive("addPet", HttpStatus.CREATED));
    }

    @DataProvider(name="pingApiOperationDataprovider")
    public static Object[][] pingApiOperationDataprovider() {
        return new Object[][]{{"doPing"}, {"doPong"}, {"doPung"}};
    }

    @Test(dataProvider = "pingApiOperationDataprovider")
    @CitrusTest
    @Ignore  // Solve issue with composite schemes
    public void shouldPerformRoundtripPingOperation(String pingApiOperation) {

        variable("id", 2001);
        when(openapi(pingSpec)
            .client(httpClient)
            .send(pingApiOperation)
            .message()
            .fork(true));

        then(openapi(pingSpec).server(httpServer)
            .receive(pingApiOperation)
            .message()
            .accept("@contains('application/json')@"));

        then(openapi(pingSpec).server(httpServer)
            .send(pingApiOperation)
            .message()
            .contentType("application/json"));

        OpenApiClientResponseActionBuilder clientResponseActionBuilder = openapi(pingSpec)
            .client(httpClient).receive(pingApiOperation, HttpStatus.OK);

       then(clientResponseActionBuilder);
    }
}
