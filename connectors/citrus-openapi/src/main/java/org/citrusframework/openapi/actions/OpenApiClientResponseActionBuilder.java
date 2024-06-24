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

package org.citrusframework.openapi.actions;

import io.apicurio.datamodels.openapi.models.OasOperation;
import io.apicurio.datamodels.openapi.models.OasResponse;
import io.apicurio.datamodels.openapi.models.OasSchema;
import jakarta.annotation.Nullable;
import org.citrusframework.CitrusSettings;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.http.actions.HttpClientResponseActionBuilder;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.message.HttpMessageBuilder;
import org.citrusframework.message.Message;
import org.citrusframework.message.MessageType;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.OpenApiTestDataGenerator;
import org.citrusframework.openapi.model.OasModelHelper;
import org.citrusframework.openapi.model.OperationPathAdapter;
import org.citrusframework.openapi.validation.OpenApiResponseValidationProcessor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import java.util.Collection;
import java.util.Map;
import java.util.Optional;
import java.util.regex.Pattern;

/**
 * @author Christoph Deppisch
 * @since 4.1
 */
public class OpenApiClientResponseActionBuilder extends HttpClientResponseActionBuilder {

    private final OpenApiResponseValidationProcessor openApiResponseValidationProcessor;

    /**
     * Default constructor initializes http response message builder.
     */
    public OpenApiClientResponseActionBuilder(OpenApiSpecification openApiSpec, String operationId,
        String statusCode) {
        this(new HttpMessage(), openApiSpec, operationId, statusCode);
    }

    public OpenApiClientResponseActionBuilder(HttpMessage httpMessage,
        OpenApiSpecification openApiSpec,
        String operationId, String statusCode) {
        super(new OpenApiClientResponseMessageBuilder(httpMessage, openApiSpec, operationId,
            statusCode), httpMessage);

        openApiResponseValidationProcessor = new OpenApiResponseValidationProcessor(openApiSpec, operationId);
        validate(openApiResponseValidationProcessor);
    }

    public OpenApiClientResponseActionBuilder disableOasValidation(boolean b) {
        if (openApiResponseValidationProcessor != null) {
            openApiResponseValidationProcessor.setEnabled(!b);
        }
        return this;
    }

    public static void fillMessageFromResponse(OpenApiSpecification openApiSpecification,
        TestContext context, HttpMessage httpMessage, @Nullable OasOperation operation,
        @Nullable OasResponse response) {

        if (operation == null || response == null) {
            return;
        }

        fillRequiredHeaders(
            openApiSpecification, context, httpMessage, response);

        Optional<OasSchema> responseSchema = OasModelHelper.getSchema(response);
        responseSchema.ifPresent(oasSchema -> {
                httpMessage.setPayload(
                    OpenApiTestDataGenerator.createInboundPayload(oasSchema,
                        OasModelHelper.getSchemaDefinitions(
                            openApiSpecification.getOpenApiDoc(context)), openApiSpecification));

                // Best guess for the content type. Currently, we can only determine the content type
                // for sure for json. Other content types will be neglected.
                OasSchema resolvedSchema = OasModelHelper.resolveSchema(
                    openApiSpecification.getOpenApiDoc(null), oasSchema);
                if (OasModelHelper.isObjectType(resolvedSchema) || OasModelHelper.isObjectArrayType(
                    resolvedSchema)) {
                    Collection<String> responseTypes = OasModelHelper.getResponseTypes(operation,
                        response);
                    if (responseTypes.contains(MediaType.APPLICATION_JSON_VALUE)) {
                        httpMessage.setHeader(HttpHeaders.CONTENT_TYPE,
                            MediaType.APPLICATION_JSON_VALUE);
                        httpMessage.setType(MessageType.JSON);
                    }
                }
            }
        );

    }

    private static void fillRequiredHeaders(
        OpenApiSpecification openApiSpecification, TestContext context, HttpMessage httpMessage,
        OasResponse response) {

        Map<String, OasSchema> requiredHeaders = OasModelHelper.getRequiredHeaders(response);
        for (Map.Entry<String, OasSchema> header : requiredHeaders.entrySet()) {
            httpMessage.setHeader(header.getKey(),
                OpenApiTestDataGenerator.createValidationExpression(header.getKey(),
                    header.getValue(),
                    OasModelHelper.getSchemaDefinitions(
                        openApiSpecification.getOpenApiDoc(context)), false,
                    openApiSpecification,
                    context));
        }

        Map<String, OasSchema> headers = OasModelHelper.getHeaders(response);
        for (Map.Entry<String, OasSchema> header : headers.entrySet()) {
            if (!requiredHeaders.containsKey(header.getKey()) && context.getVariables()
                .containsKey(header.getKey())) {
                httpMessage.setHeader(header.getKey(),
                    CitrusSettings.VARIABLE_PREFIX + header.getKey()
                        + CitrusSettings.VARIABLE_SUFFIX);
            }
        }
    }

    private static class OpenApiClientResponseMessageBuilder extends HttpMessageBuilder {

        private final OpenApiSpecification openApiSpec;
        private final String operationId;
        private final String statusCode;

        private final HttpMessage httpMessage;

        public OpenApiClientResponseMessageBuilder(HttpMessage httpMessage,
            OpenApiSpecification openApiSpec,
            String operationId, String statusCode) {
            super(httpMessage);
            this.openApiSpec = openApiSpec;
            this.operationId = operationId;
            this.statusCode = statusCode;
            this.httpMessage = httpMessage;
        }

        @Override
        public Message build(TestContext context, String messageType) {

            openApiSpec.getOperation(operationId, context).ifPresentOrElse(operationPathAdapter ->
                buildMessageFromOperation(operationPathAdapter, context), () -> {
                throw new CitrusRuntimeException("Unable to locate operation with id '%s' in OpenAPI specification %s".formatted(operationId, openApiSpec.getSpecUrl()));
            });

            return super.build(context, messageType);
        }

        private void buildMessageFromOperation(OperationPathAdapter operationPathAdapter, TestContext context) {
            OasOperation operation = operationPathAdapter.operation();

            if (operation.responses != null) {
                Optional<OasResponse> responseForRandomGeneration = OasModelHelper.getResponseForRandomGeneration(
                    openApiSpec.getOpenApiDoc(context), operation, statusCode, null);

                responseForRandomGeneration.ifPresent(
                    oasResponse -> fillMessageFromResponse(openApiSpec, context, httpMessage,
                        operation, oasResponse));
            }

            if (Pattern.compile("\\d+").matcher(statusCode).matches()) {
                httpMessage.status(HttpStatus.valueOf(Integer.parseInt(statusCode)));
            } else {
                httpMessage.status(HttpStatus.OK);
            }
        }
    }
}
