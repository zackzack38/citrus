/** 
 * ==================================================
 * GENERATED CLASS, ALL CHANGES WILL BE LOST
 * ==================================================
 */

package org.citrusframework.openapi.generator.soap.bookservice.citrus;

import jakarta.annotation.Generated;
import java.util.List;
import java.util.ServiceLoader;
import org.citrusframework.actions.AbstractTestAction;
import org.citrusframework.context.TestContext;
import org.citrusframework.http.actions.HttpClientRequestActionBuilder;
import org.citrusframework.testapi.ApiActionBuilderCustomizerService;
import org.citrusframework.testapi.GeneratedApi;
import org.citrusframework.spi.Resources;
import org.citrusframework.validation.DelegatingPayloadVariableExtractor;
import org.citrusframework.validation.PathExpressionValidationContext;
import org.citrusframework.validation.script.ScriptValidationContext;
import org.citrusframework.ws.actions.ReceiveSoapMessageAction;
import org.citrusframework.ws.actions.ReceiveSoapMessageAction.SoapMessageBuilderSupport;
import org.citrusframework.ws.actions.SendSoapMessageAction;
import org.citrusframework.ws.actions.SoapActionBuilder;
import org.citrusframework.ws.client.WebServiceClient;
import org.slf4j.Marker;
import org.slf4j.MarkerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.util.CollectionUtils;

import javax.sql.DataSource;
import java.util.Map;

@Generated(value = "org.citrusframework.openapi.generator.JavaCitrusCodegen")
public abstract class OpenApiFromWsdlAbstractTestRequest extends AbstractTestAction {

    protected final Marker coverageMarker = MarkerFactory.getMarker("OPENAPIFROMWSDL-API-COVERAGE");

    @Autowired
    @Qualifier("soapSampleEndpoint")
    protected WebServiceClient wsClient;

    @Autowired(required = false)
    protected DataSource dataSource;

    @Autowired(required = false)
    private List<ApiActionBuilderCustomizerService> actionBuilderCustomizerServices;

    // attributes of differentNodes
    protected String bodyContentType;
    protected String bodyLiteralContentType;
    protected String bodyFile;
    protected String bodyLiteral;

    // children of response element
    protected String resource;
    protected Map<String, String> responseVariable; // Contains the 'XPATH' as key and the 'VARIABLE NAME' as value
    protected Map<String, String> responseValue; // Contains the 'XPATH' as key and the 'VALUE TO BE VALIDATED' as value
    protected String script;
    protected String type; // default script type is groovy - supported types see com.consol.citrus.script.ScriptTypes
    protected Map<String, String> soapHeaders;
    protected Map<String, String> mimeHeaders;
    
    @Override
    public void doExecute(TestContext context) {
        sendRequest(context);
        receiveResponse(context);
    }

    /**
     * This method receives the HTTP-Response
     */
    public void receiveResponse(TestContext context) {

        ReceiveSoapMessageAction.Builder soapReceiveMessageActionBuilder = new SoapActionBuilder().client(wsClient).receive();
        SoapMessageBuilderSupport messageBuilderSupport = soapReceiveMessageActionBuilder.getMessageBuilderSupport();

        if (resource != null) {
            messageBuilderSupport.body(Resources.create(resource));
        }

        if (!CollectionUtils.isEmpty(responseVariable)) {
            DelegatingPayloadVariableExtractor.Builder extractorBuilder = new DelegatingPayloadVariableExtractor.Builder();
            responseVariable.forEach(extractorBuilder::expression);
            messageBuilderSupport.extract(extractorBuilder);
        }

        if (!CollectionUtils.isEmpty(responseValue)) {
            PathExpressionValidationContext.Builder validationContextBuilder = new PathExpressionValidationContext.Builder();
            responseValue.forEach(validationContextBuilder::expression);
            messageBuilderSupport.validate(validationContextBuilder);
        }

        if (script != null) {
            ScriptValidationContext.Builder scriptValidationContextBuilder = new ScriptValidationContext.Builder();
            if (type != null) {
                scriptValidationContextBuilder.scriptType(type);
            }
            scriptValidationContextBuilder.script(script);
            messageBuilderSupport.validate(scriptValidationContextBuilder);
        }

        soapReceiveMessageActionBuilder.withReferenceResolver(context.getReferenceResolver());
        soapReceiveMessageActionBuilder.build().execute(context);
    }

    public abstract void sendRequest(TestContext context);

    public void setBodyLiteral(String bodyLiteral) {
        this.bodyLiteral = bodyLiteral;
    }
    public void setBodyContentType(String bodyContentType) {
        this.bodyContentType = bodyContentType;
    }

    public void setBodyLiteralContentType(String bodyLiteralContentType) {
        this.bodyLiteralContentType = bodyLiteralContentType;
    }

    public void setBodyFile(String bodyFile) {
        this.bodyFile = bodyFile;
    }

    public void setResource(String resource) {
        this.resource = resource;
    }

    public void setResponseVariable(Map<String, String> responseVariable) {
        this.responseVariable = responseVariable;
    }

    public void setResponseValue(Map<String, String> responseValue) {
        this.responseValue = responseValue;
    }

    public void setScript(String script) {
        this.script = script;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setSoapHeader(Map<String, String> soapHeaders) {
        this.soapHeaders = soapHeaders;
    }

    public void setMimeHeader(Map<String, String> mimeHeaders) {
        this.mimeHeaders = mimeHeaders;
    }

    protected SendSoapMessageAction.Builder customizeBuilder(GeneratedApi generatedApi,
        TestContext context, SendSoapMessageAction.Builder sendSoapMessageActionBuilder) {

        sendSoapMessageActionBuilder = customizeByBeans(generatedApi, context, sendSoapMessageActionBuilder);

        sendSoapMessageActionBuilder = customizeBySpi(generatedApi, context, sendSoapMessageActionBuilder);

        return sendSoapMessageActionBuilder;
    }

    private SendSoapMessageAction.Builder customizeBySpi(GeneratedApi generatedApi, TestContext context,
        SendSoapMessageAction.Builder sendSoapMessageActionBuilder) {

        ServiceLoader<ApiActionBuilderCustomizerService> serviceLoader = ServiceLoader.load(
            ApiActionBuilderCustomizerService.class, ApiActionBuilderCustomizerService.class.getClassLoader());
        for (ApiActionBuilderCustomizerService service :serviceLoader) {
            sendSoapMessageActionBuilder = service.build(generatedApi, this, context, sendSoapMessageActionBuilder);
        }

        return sendSoapMessageActionBuilder;
    }

    private SendSoapMessageAction.Builder customizeByBeans(
        GeneratedApi generatedApi, TestContext context, SendSoapMessageAction.Builder sendSoapMessageActionBuilder) {

        if (actionBuilderCustomizerServices != null) {
            for (ApiActionBuilderCustomizerService apiActionBuilderCustomizer : actionBuilderCustomizerServices) {
                sendSoapMessageActionBuilder = apiActionBuilderCustomizer.build(generatedApi, this,
                    context, sendSoapMessageActionBuilder);
            }
        }

        return sendSoapMessageActionBuilder;
    }
}
