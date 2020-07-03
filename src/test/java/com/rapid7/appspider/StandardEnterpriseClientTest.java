package com.rapid7.appspider;

import java.io.IOException;
import java.util.Optional;

import org.apache.http.*;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import javax.ws.rs.core.MediaType;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class StandardEnterpriseClientTest {

    private LoggerFacade logger;
    private ContentHelper contentHelper;
    private ApiSerializer apiSerializer;
    private HttpClient httpClient;

    private static final String url = "https://appspider.rapid7.com/AppSpiderEnterprise/rest/v1";

    @BeforeEach
    public void initialize() {
        logger = mock(LoggerFacade.class);
        contentHelper = new ContentHelper(logger);
        apiSerializer = new ApiSerializer(logger);

        //httpClient = new HttpClientFactory().getClient(); // uncommenting if htting a real server
        httpClient = mock(HttpClient.class);
    }

    @AfterEach
    public void cleanup() {
        // provided in case we're using an actual connection for testing
        if (httpClient instanceof CloseableHttpClient) {
            CloseableHttpClient closeMe = (CloseableHttpClient)httpClient;
            try {
                closeMe.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Test
    public void testAuthentication_returnsTrueWhenCredentialsAreValid() throws IOException {
        arrangeLogin(true, "abcdef");
        EnterpriseClient client = new StandardEnterpriseClient(new HttpClientService(httpClient, contentHelper, logger), url, apiSerializer, contentHelper, logger);

        assertTrue(client.testAuthentication("wolf359", "pa55word"));
    }

    @Test
    public void testAuthentication_returnsFalseWhenCredentialsAreInvalid() throws IOException {
        arrangeLogin(false, "");
        EnterpriseClient client = new StandardEnterpriseClient(new HttpClientService(httpClient, contentHelper, logger), url, apiSerializer, contentHelper, logger);

        // act and assert
        assertFalse(client.testAuthentication("wolf359", "pa55word"));
    }

    @Test
    public void login_hasTokenWhenCredentialsAreValid() throws IOException {
        arrangeLogin(true, "abcdef");
        EnterpriseClient client = new StandardEnterpriseClient(new HttpClientService(httpClient, contentHelper, logger), url, apiSerializer, contentHelper, logger);
        assertTrue(client.login("wolf359", "pa55word").isPresent());
    }

    @Test
    public void login_TokenHasExpectedValueWhenCredentialsAreValid() throws IOException {
        String token = "abcdef";
        arrangeLogin(true, token);
        EnterpriseClient client = new StandardEnterpriseClient(new HttpClientService(httpClient, contentHelper, logger), url, apiSerializer, contentHelper, logger);
        Optional<String> actualToken = client.login("wolf359", "pa55word");
        assertEquals(token, actualToken.get());
    }

    @Test
    public void login_tokenNotPresentWhenCredentialsAreInvalid() throws IOException {
        arrangeLogin(false, "");
        EnterpriseClient client = new StandardEnterpriseClient(new HttpClientService(httpClient, contentHelper, logger), url, apiSerializer, contentHelper, logger);
        assertFalse(client.login("wolf359", "pa55word").isPresent());
    }


    private StandardEnterpriseClientTest arrangeLogin(boolean isSuccess, String token) throws IOException {
        ArgumentMatcher<HttpRequestBase> requestMatcher = request ->
            request instanceof HttpPost && request.getHeaders(HttpHeaders.CONTENT_TYPE)[0].getValue().equals(MediaType.APPLICATION_JSON);
        HttpResponse response = getAuthenticationResult(isSuccess, token);
        when(httpClient.execute(argThat(requestMatcher))).thenReturn(response);
        return this;
    }

    private static HttpResponse getAuthenticationResult(boolean isSuccess, String token) {
        JSONObject jsonObject = new JSONObject();
        jsonObject.put("IsSuccess", isSuccess ? "true" : "false");
        jsonObject.put("Token", token);
        StringEntity entity = new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);
        HttpResponse response = mock(HttpResponse.class);
        StatusLine status = mock(StatusLine.class);
        when(status.getStatusCode()).thenReturn(isSuccess ? 200 : 500);
        when(response.getStatusLine()).thenReturn(status);
        when(response.getEntity()).thenReturn(entity);
        return response;
    }
}
