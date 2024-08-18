package org.citrusframework.openapi.generator;

import org.citrusframework.http.actions.HttpClientRequestActionBuilder.HttpMessageBuilderSupport;
import org.citrusframework.util.StringUtils;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

class TestApiUtilsTest {

    @Test
    void shouldAddBasicAuthHeaderWhenUsernameAndPasswordAreProvided() {
        // Given
        String username = "user";
        String password = "pass";
        HttpMessageBuilderSupport messageBuilderSupport = mock(HttpMessageBuilderSupport.class);

        // When
        TestApiUtils.addBasicAuthHeader(username, password, messageBuilderSupport);

        // Then
        verify(messageBuilderSupport).header(eq("Authorization"), eq("Basic citrus:encodeBase64(user:pass)"));
    }

    @Test
    void shouldNotAddBasicAuthHeaderWhenUsernameIsEmpty() {
        // Given
        String username = "";
        String password = "pass";
        HttpMessageBuilderSupport messageBuilderSupport = mock(HttpMessageBuilderSupport.class);

        // When
        TestApiUtils.addBasicAuthHeader(username, password, messageBuilderSupport);

        // Then
        verify(messageBuilderSupport, never()).header(anyString(), anyString());
    }

    @Test
    void shouldNotAddBasicAuthHeaderWhenPasswordIsEmpty() {
        // Given
        String username = "user";
        String password = "";
        HttpMessageBuilderSupport messageBuilderSupport = mock(HttpMessageBuilderSupport.class);

        // When
        TestApiUtils.addBasicAuthHeader(username, password, messageBuilderSupport);

        // Then
        verify(messageBuilderSupport, never()).header(anyString(), anyString());
    }

    @Test
    void shouldNotAddBasicAuthHeaderWhenBothUsernameAndPasswordAreEmpty() {
        // Given
        String username = "";
        String password = "";
        HttpMessageBuilderSupport messageBuilderSupport = mock(HttpMessageBuilderSupport.class);

        // When
        TestApiUtils.addBasicAuthHeader(username, password, messageBuilderSupport);

        // Then
        verify(messageBuilderSupport, never()).header(anyString(), anyString());
    }
}
