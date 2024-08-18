package org.citrusframework.openapi.generator;

import org.citrusframework.http.actions.HttpClientRequestActionBuilder.HttpMessageBuilderSupport;
import org.citrusframework.util.StringUtils;

public class TestApiUtils {

    private TestApiUtils() {
        //prevent instantiation of utility class
    }

    public static void addBasicAuthHeader(String username, String password, HttpMessageBuilderSupport messageBuilderSupport) {
        if (!StringUtils.isEmpty(username) && !StringUtils.isEmpty(password)) {
            messageBuilderSupport.header("Authorization",
                "Basic citrus:encodeBase64(" + username + ":" + password + ")");
        }
    }

    public static String mapXmlAttributeNameToJavaPropertyName(String attributeName) {

        if (StringUtils.isEmpty(attributeName)) {
            return attributeName;
        }

        if ("basicUsername".equals(attributeName)) {
            return "withBasicAuthUsername";
        } else if ("basicPassword".equals(attributeName)) {
            return "withBasicAuthPassword";
        }

        return "with" + attributeName.substring(0,0).toUpperCase()+attributeName.substring(1);

    }

}
