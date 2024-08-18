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
import io.apicurio.datamodels.openapi.models.OasParameter;
import io.apicurio.datamodels.openapi.models.OasSchema;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.regex.Pattern;
import org.citrusframework.CitrusSettings;
import org.citrusframework.actions.SendMessageAction;
import org.citrusframework.context.TestContext;
import org.citrusframework.exceptions.CitrusRuntimeException;
import org.citrusframework.http.actions.HttpClientRequestActionBuilder;
import org.citrusframework.http.message.HttpMessage;
import org.citrusframework.http.message.HttpMessageBuilder;
import org.citrusframework.message.Message;
import org.citrusframework.openapi.OpenApiMessageType;
import org.citrusframework.openapi.OpenApiSpecification;
import org.citrusframework.openapi.OpenApiTestDataGenerator;
import org.citrusframework.openapi.model.OasModelHelper;
import org.citrusframework.openapi.model.OperationPathAdapter;
import org.citrusframework.openapi.validation.OpenApiMessageProcessor;
import org.citrusframework.util.StringUtils;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * @author Christoph Deppisch
 * @since 4.1
 */
public class OpenApiClientRequestActionBuilder extends HttpClientRequestActionBuilder {

    private OpenApiMessageProcessor openApiMessageProcessor;

    private final OpenApiSpecificationSource openApiSpecificationSource;

    private final String operationId;

    private boolean oasValidationEnabled = true;

    /**
     * Default constructor initializes http request message builder.
     */
    public OpenApiClientRequestActionBuilder(OpenApiSpecificationSource openApiSpec,
        String operationId) {
        this(new HttpMessage(), openApiSpec, operationId);
    }

    public OpenApiClientRequestActionBuilder(HttpMessage httpMessage,
        OpenApiSpecificationSource openApiSpec,
        String operationId) {
        super(new OpenApiClientRequestMessageBuilder(httpMessage, openApiSpec, operationId),
            httpMessage);

        this.openApiSpecificationSource = openApiSpec;
        this.operationId = operationId;
    }

    @Override
    public SendMessageAction doBuild() {
        OpenApiSpecification openApiSpecification = openApiSpecificationSource.resolve(
            referenceResolver);
        if (oasValidationEnabled && !messageProcessors.contains(
            openApiMessageProcessor)) {
            openApiMessageProcessor = new OpenApiMessageProcessor(openApiSpecification, operationId,
                OpenApiMessageType.REQUEST);
            process(openApiMessageProcessor);
        }

        return super.doBuild();
    }

    /**
     * By default, enable schema validation as the OpenAPI is always available.
     */
    @Override
    protected HttpMessageBuilderSupport createHttpMessageBuilderSupport() {
        HttpMessageBuilderSupport httpMessageBuilderSupport = super.createHttpMessageBuilderSupport();
        httpMessageBuilderSupport.schemaValidation(true);
        return httpMessageBuilderSupport;
    }

    public OpenApiClientRequestActionBuilder disableOasValidation(boolean disabled) {
        oasValidationEnabled = !disabled;
        return this;
    }



}
