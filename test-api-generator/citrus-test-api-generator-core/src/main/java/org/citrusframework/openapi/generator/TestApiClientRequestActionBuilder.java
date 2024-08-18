package org.citrusframework.openapi.generator;

import jakarta.servlet.http.Cookie;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.citrusframework.CitrusSettings;
import org.citrusframework.actions.SendMessageAction;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.message.Message;
import org.citrusframework.message.MessageBuilder;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.actions.OpenApiClientRequestActionBuilder;
import org.citrusframework.openapi.actions.OpenApiClientRequestMessageBuilder;
import org.citrusframework.openapi.actions.OpenApiSpecificationSource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

public class TestApiClientRequestActionBuilder extends OpenApiClientRequestActionBuilder {

    // TODO: do we really need this?
    protected OpenApiSpecification openApiSpec;

    private final String path;

    private String basicUsername;

    private String basicPassword;

    // TODO: can we just pass in the operation?
    public TestApiClientRequestActionBuilder(OpenApiSpecification openApiSpec, String method,
        String path, String operationName) {
        super(new OpenApiSpecificationSource(openApiSpec), "%s_%s".formatted(method, path));
        name(String.format("send-%s:%s", "PetStore".toLowerCase(), operationName));
        getMessageBuilderSupport().header("citrus_open_api_operation_name", operationName);
        getMessageBuilderSupport().header("citrus_open_api_method", method);
        getMessageBuilderSupport().header("citrus_open_api_path", path);

        this.openApiSpec = openApiSpec;
        this.path = path;
    }

    protected void pathParameter(String name, Object value) {
        ((TestApiClientRequestMessageActionBuilder)getMessageBuilderSupport().getMessageBuilder())
            .pathParameter(name, value);
    }

    protected void queryParameter(String name, Object value) {
        if (value instanceof Collection<?> collection) {
            collection.forEach(val -> super.queryParam(name, val != null ? val.toString() : null));
        } else {
            super.queryParam(name, value != null ? value.toString() : null);
        }
    }

    protected void formParameter(String name, String value) {
        ((TestApiClientRequestMessageActionBuilder)getMessageBuilderSupport().getMessageBuilder())
            .formParameter(name, value);

    }

    protected void headerParameter(String name, String value) {
        getMessageBuilderSupport().header(name, value);
    }

    protected void cookieParameter(String name, String value) {
        // TODO: is this actually called. Is there a test for it?
        getMessageBuilderSupport().cookie(new Cookie(name, value));
    }



    protected String toQueryParameter(String... arrayElements) {
        return String.join(",", arrayElements);
    }

    @Override
    public SendMessageAction doBuild() {
        // TODO: register callback to modify builder
        return super.doBuild();
    }

    public class TestApiClientRequestMessageActionBuilder extends
        OpenApiClientRequestMessageBuilder {

        private final Map<String, String> pathParameters = new HashMap<>();

        private final MultiValueMap<String, Object> formParameters = new LinkedMultiValueMap<>();

        public TestApiClientRequestMessageActionBuilder(
            HttpMessage httpMessage,
            OpenApiSpecificationSource openApiSpec, String operationId) {
            super(httpMessage, openApiSpec, operationId);
        }

        protected void pathParameter(String name, Object value) {

            // TODO: consider types for value. Is value restricted to native types? can it be a collection or array?
            if (value == null) {
                throw new CitrusRuntimeException(
                    "Mandatory path parameter '%s' must not be null".formatted(name));
            }
            pathParameters.put(name, value.toString());
        }

        protected void formParameter(String name, Object value) {
            formParameters.add(name, value);
        }

        protected String getDefinedPathParameter(TestContext context, String name) {
            return pathParameters.getOrDefault(name, super.getDefinedPathParameter(context, name));
        }

        @Override
        public Message build(TestContext context, String messageType) {
            path(replaceParameterPlaceholders(path));

            if (!formParameters.isEmpty()) {
                // TODO: do we have to explicitly set the content type or is this done by citrus
                messageBuilderSupport.contentType(MediaType.MULTIPART_FORM_DATA_VALUE);
                getMessageBuilderSupport().body(formParameters);
            }
            return super.build(context, messageType);
        }

        private String replaceParameterPlaceholders(String path) {

            String qualifiedPath = path;
            for (Entry<String, String> entry : pathParameters.entrySet()) {
                qualifiedPath = qualifiedPath.replace("{%s}".formatted(entry.getKey()),
                    entry.getValue());
            }
            return qualifiedPath;
        }
    }
}