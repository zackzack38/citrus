package org.citrusframework.openapi.actions;

import io.apicurio.datamodels.openapi.models.OasOperation;
import io.apicurio.datamodels.openapi.models.OasParameter;
import io.apicurio.datamodels.openapi.models.OasSchema;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.citrusframework.CitrusSettings;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.message.HttpMessageBuilder;
import org.citrusframework.message.Message;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.OpenApiTestDataGenerator;
import org.citrusframework.openapi.model.OasModelHelper;
import org.citrusframework.openapi.model.OperationPathAdapter;
import org.citrusframework.util.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

public class OpenApiClientRequestMessageBuilder extends HttpMessageBuilder {

        private final OpenApiSpecificationSource openApiSpecificationSource;

        private final String operationId;

        private final HttpMessage httpMessage;

        public OpenApiClientRequestMessageBuilder(HttpMessage httpMessage,
            OpenApiSpecificationSource openApiSpec,
            String operationId) {
            super(httpMessage);
            this.openApiSpecificationSource = openApiSpec;
            this.operationId = operationId;
            this.httpMessage = httpMessage;
        }

        @Override
        public Message build(TestContext context, String messageType) {
            OpenApiSpecification openApiSpecification = openApiSpecificationSource.resolve(
                context.getReferenceResolver());
            openApiSpecification.getOperation(operationId, context)
                .ifPresentOrElse(operationPathAdapter ->
                        buildMessageFromOperation(openApiSpecification, operationPathAdapter, context),
                    () -> {
                        throw new CitrusRuntimeException(
                            "Unable to locate operation with id '%s' in OpenAPI specification %s".formatted(
                                operationId, openApiSpecification.getSpecUrl()));
                    });

            return super.build(context, messageType);
        }

        private void buildMessageFromOperation(OpenApiSpecification openApiSpecification,
            OperationPathAdapter operationPathAdapter, TestContext context) {
            OasOperation operation = operationPathAdapter.operation();
            String path = operationPathAdapter.apiPath();
            HttpMethod method = HttpMethod.valueOf(
                operationPathAdapter.operation().getMethod().toUpperCase(Locale.US));

            if (operation.parameters != null) {
                setMissingRequiredHeadersToRandomValues(openApiSpecification, context, operation);
                setMissingRequiredQueryParametersToRandomValues(context, operation);
            }

            setMissingRequiredBodyToRandomValue(openApiSpecification, context, operation);

            String randomizedPath = path;
            if (operation.parameters != null) {
                List<OasParameter> pathParams = operation.parameters.stream()
                    .filter(p -> "path".equals(p.in)).toList();

                for (OasParameter parameter : pathParams) {
                    String parameterValue;
                    String pathParameterValue = getDefinedPathParameter(context, parameter.getName());
                    if (StringUtils.isEmpty(pathParameterValue)) {
                        parameterValue = "\\" + pathParameterValue;
                    } else {
                        parameterValue = OpenApiTestDataGenerator.createRandomValueExpression(
                            (OasSchema) parameter.schema);
                    }
                    randomizedPath = Pattern.compile("\\{" + parameter.getName() + "}")
                        .matcher(randomizedPath)
                        .replaceAll(parameterValue);
                }
            }

            OasModelHelper.getRequestContentType(operation)
                .ifPresent(
                    contentType -> httpMessage.setHeader(HttpHeaders.CONTENT_TYPE, contentType));

            httpMessage.path(randomizedPath);
            httpMessage.method(method);
        }

        protected String getDefinedPathParameter(TestContext context, String name) {
            if (context.getVariables().containsKey(name)) {
                return CitrusSettings.VARIABLE_PREFIX + name
                    + CitrusSettings.VARIABLE_SUFFIX;
            }
            return null;
        }

        private void setMissingRequiredBodyToRandomValue(OpenApiSpecification openApiSpecification, TestContext context, OasOperation operation) {
            if (httpMessage.getPayload() == null || (httpMessage.getPayload() instanceof String p
                && p.isEmpty())) {
                Optional<OasSchema> body = OasModelHelper.getRequestBodySchema(
                    openApiSpecification.getOpenApiDoc(context), operation);
                body.ifPresent(oasSchema -> httpMessage.setPayload(
                    OpenApiTestDataGenerator.createOutboundPayload(oasSchema,
                        openApiSpecification)));
            }
        }

        /**
         * Creates all required query parameters, if they have not already been specified.
         */
        private void setMissingRequiredQueryParametersToRandomValues(TestContext context, OasOperation operation) {
            operation.parameters.stream()
                .filter(param -> "query".equals(param.in))
                .filter(
                    param -> Boolean.TRUE.equals(param.required) || context.getVariables()
                        .containsKey(param.getName()))
                .forEach(param -> {
                    // If not already configured explicitly, create a random value
                    if (!httpMessage.getQueryParams().containsKey(param.getName())) {
                        httpMessage.queryParam(param.getName(),
                            OpenApiTestDataGenerator.createRandomValueExpression(param.getName(),
                                (OasSchema) param.schema,
                                context));
                    }
                });
        }

        /**
         * Creates all required headers, if they have not already been specified.
         */
        private void setMissingRequiredHeadersToRandomValues(OpenApiSpecification openApiSpecification,
            TestContext context, OasOperation operation) {
            List<String> configuredHeaders = getHeaderBuilders()
                .stream()
                .flatMap(b -> b.builderHeaders(context).keySet().stream())
                .toList();
            operation.parameters.stream()
                .filter(param -> "header".equals(param.in))
                .filter(
                    param -> Boolean.TRUE.equals(param.required) || context.getVariables()
                        .containsKey(param.getName()))
                .forEach(param -> {
                    // If not already configured explicitly, create a random value
                    if (httpMessage.getHeader(param.getName()) == null
                        && !configuredHeaders.contains(param.getName())) {
                        httpMessage.setHeader(param.getName(),
                            OpenApiTestDataGenerator.createRandomValueExpression(param.getName(),
                                (OasSchema) param.schema,
                                openApiSpecification, context));
                    }
                });
        }
    }