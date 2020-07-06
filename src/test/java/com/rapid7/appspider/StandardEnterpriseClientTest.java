package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ScanResult;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.json.JSONArray;
import org.json.JSONObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StandardEnterpriseClientTest {

    private LoggerFacade logger;
    private ContentHelper contentHelper;
    private ApiSerializer apiSerializer;
    private HttpClient httpClient;

    private static final String url = "https://appspider.rapid7.com/AppSpiderEnterprise/rest/v1";
    private String expectedAuthToken;
    private String configId;
    private String configName;
    private String expectedScanId;
    private EnterpriseClient client;

    @BeforeEach
    public void initialize() {
        logger = mock(LoggerFacade.class);
        contentHelper = new ContentHelper(logger);
        apiSerializer = new ApiSerializer(logger);
        expectedAuthToken = ""; // set by isSuccess state of each test, just being reset here
        expectedScanId = "";
        configId = UUID.randomUUID().toString();
        configName = "ConfigName:" + UUID.randomUUID().toString();

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
    void testAuthenticationReturnsTrueWhenCredentialsAreValid() throws IOException {
        arrangeExpectedValues(true)
            .configureLogin(true)
            .configureEnterpriseClient();

        assertTrue(client.testAuthentication("wolf359", "pa55word"));
    }

    @Test
    void testAuthenticationReturnsFalseWhenCredentialsAreInvalid() throws IOException {
        arrangeExpectedValues(false)
            .configureLogin(false)
            .configureEnterpriseClient();

        // act and assert
        assertFalse(client.testAuthentication("wolf359", "pa55word"));
    }

    @Test
    void loginHasTokenWhenCredentialsAreValid() throws IOException {
        arrangeExpectedValues(true)
            .configureLogin(true)
            .configureEnterpriseClient();
        assertTrue(client.login("wolf359", "pa55word").isPresent());
    }

    @Test
    void loginTokenHasExpectedValueWhenCredentialsAreValid() throws IOException {
        arrangeExpectedValues(true)
            .configureLogin(true)
            .configureEnterpriseClient();
        Optional<String> actualToken = client.login("wolf359", "pa55word");
        assertEquals(expectedAuthToken, actualToken.get());
    }

    @Test
    void loginTokenNotPresentWhenCredentialsAreInvalid() throws IOException {
        arrangeExpectedValues(false)
            .configureLogin(false)
            .configureEnterpriseClient();
        assertFalse(client.login("wolf359", "pa55word").isPresent());
    }

    @Test
    void runScanConfigByNameIsSuccessWhenConfigFoundAndScanCreated() throws IOException {
        arrangeExpectedValues(true)
            .configureGetConfigs(true)
            .configureRunScanByConfigId(true)
            .configureEnterpriseClient();

        ScanResult scanResult = client.runScanByConfigName(expectedAuthToken, configName);

        assertTrue(scanResult.isSuccess());
    }

    @Test
    void runScanConfigByNameHasScanIdWhenConfigFoundAndScanCreated() throws IOException {
        arrangeExpectedValues(true)
            .configureGetConfigs(true)
            .configureRunScanByConfigId(true)
            .configureEnterpriseClient();
        ScanResult scanResult = client.runScanByConfigName(expectedAuthToken, configName);

        assertEquals(expectedScanId, scanResult.getScanId());
    }

    private static final String AUTHENTICATION_LOGIN = "/Authentication/Login";
    private static final String GET_CONFIGS = "/Config/GetConfigs";
    private static final String RUN_SCAN = "/Scan/RunScan";

    private StandardEnterpriseClientTest arrangeExpectedValues(boolean isSuccess) {
        if (isSuccess) {
            expectedAuthToken = UUID.randomUUID().toString();
            expectedScanId = UUID.randomUUID().toString();
        } else {
            expectedAuthToken = "";
            expectedScanId = "";
        }
        return this;
    }
    private StandardEnterpriseClientTest configureLogin(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> loginRequest = request -> request instanceof HttpPost &&  request.getURI().toString().equals(url + AUTHENTICATION_LOGIN);
        HttpResponse response = getAuthenticationResult(isSuccess, expectedAuthToken);
        when(httpClient.execute(argThat(loginRequest))).thenReturn(response);
        return this;
    }

    private StandardEnterpriseClientTest configureGetConfigs(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getConfigsRequest = request -> request instanceof HttpGet &&  request.getURI().toString().equals(url + GET_CONFIGS);
        HttpResponse response = getConfigNamesResponse(isSuccess, getJsonFromEntries(getEntryFrom("Id", configId), getEntryFrom("Name", configName)));
        when(httpClient.execute(argThat(getConfigsRequest))).thenReturn(response);
        return this;
    }
    private StandardEnterpriseClientTest configureRunScanByConfigId(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> loginRequest = request -> request instanceof HttpPost &&  request.getURI().toString().equals(url + RUN_SCAN);
        HttpResponse response = getRunScanByConfigIdResponse(isSuccess, expectedScanId);
        when(httpClient.execute(argThat(loginRequest))).thenReturn(response);
        return this;
    }
    private StandardEnterpriseClientTest configureEnterpriseClient() {
        client = new StandardEnterpriseClient(new HttpClientService(httpClient, contentHelper, logger), url, apiSerializer, contentHelper, logger);
        return this;
    }

    private static HttpResponse getAuthenticationResult(boolean isSuccess, String token) {
        return getMockJsonResponseFromEntries(isSuccess, getEntryFrom("IsSuccess", isSuccess ? "true" : "false"), getEntryFrom("Token", token));
    }
    @SafeVarargs
    private static HttpResponse getConfigNamesResponse(boolean isSuccess, JSONObject... objects) {
        JSONArray jsonArray = new JSONArray();
        Arrays.stream(objects)
            .forEach(jsonArray::put);
        return getMockJsonResponseFromEntries(isSuccess, getEntryFrom("Configs", jsonArray));
    }
    private static HttpResponse getRunScanByConfigIdResponse(boolean isSuccess, String expectedScanId) {
        JSONObject scan = getJsonFromEntries(getEntryFrom("Id", expectedScanId));
        JSONObject result = getJsonFromEntries(getEntryFrom("IsSuccess", isSuccess), getEntryFrom("Scan", scan));
        return getMockJsonResponseFromJSON(isSuccess, result);
    }
    private static Map.Entry<String, Object> getEntryFrom(String key, Object value) {
       return new AbstractMap.SimpleEntry<String, Object>(key, value);
    }
    private static HttpResponse getMockJsonResponseFromJSON(boolean isSuccess, JSONObject jsonObject) {
        return getMockResponseFromEntity(isSuccess, new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON));
    }
    @SafeVarargs
    private static HttpResponse getMockJsonResponseFromEntries(boolean isSuccess, Map.Entry<String, Object>... entries) {
        return getMockJsonResponseFromJSON(isSuccess, getJsonFromEntries(entries));
    }
    @SafeVarargs
    private static JSONObject getJsonFromEntries(Map.Entry<String, Object>... entries) {
        JSONObject jsonObject = new JSONObject();
        Arrays.stream(entries).forEach(entry -> jsonObject.put(entry.getKey(), entry.getValue()));
        return jsonObject;
    }

    private static HttpResponse getMockResponseFromEntity(boolean isSuccess, HttpEntity entity) {
        HttpResponse response = mock(HttpResponse.class);
        when(response.getEntity()).thenReturn(entity);

        StatusLine status = mock(StatusLine.class);
        when(status.getStatusCode()).thenReturn(isSuccess ? 200 : 500);
        when(response.getStatusLine()).thenReturn(status);

        return response;
    }
}
