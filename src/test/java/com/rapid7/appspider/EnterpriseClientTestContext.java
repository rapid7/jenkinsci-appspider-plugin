/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.mockito.ArgumentMatcher;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.rapid7.appspider.Utility.toStringArray;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class EnterpriseClientTestContext implements AutoCloseable {
    private static final String AUTHENTICATION_LOGIN = "/Authentication/Login";
    private static final String GET_ALL_ENGINE_GROUPS = "/EngineGroup/GetAllEngineGroups";
    private static final String GET_ENGINE_GROUPS_FOR_CLIENT = "/EngineGroup/GetEngineGroupsForClient";
    private static final String GET_CONFIGS = "/Config/GetConfigs";
    private static final String RUN_SCAN = "/Scan/RunScan";
    private static final String GET_SCAN_STATUS = "/Scan/GetScanStatus";
    private static final String IS_SCAN_FINISHED = "/Scan/IsScanFinished";
    private static final String HAS_REPORT = "/Scan/HasReport";
    private static final String SAVE_CONFIG = "/Config/SaveConfig";
    private static final String GET_VULNERABILITIES_SUMMARY = "/Report/GetVulnerabilitiesSummaryXml";
    private static final String GET_REPORT_ZIP = "/Report/GetReportZip";
    private static final String GET_CLIENTS = "/Client/GetClients";

    private final LoggerFacade mockLogger;
    private final ContentHelper mockContentHelper;
    private final ApiSerializer mockApiSerializer;
    private final HttpClient mockHttpClient;

    private String expectedAuthToken;
    private String configId;
    private String configName;
    private String clientId;
    private String clientName;
    private String expectedScanId;
    private EnterpriseClient enterpriseClient;
    private String url;
    private Map<String, String> expectedEngineGroupsIdsByName;
    private Map<String, String> expectedEngineGroupsNamesForClient;
    private final List<EngineStub> engineGroupDetails;

    EnterpriseClientTestContext(String url) {
        this.url = url;
        mockLogger = mock(LoggerFacade.class);
        mockContentHelper = new ContentHelper(mockLogger);
        mockApiSerializer = new ApiSerializer(mockLogger);
        expectedAuthToken = ""; // set by isSuccess state of each test, just being reset here
        expectedScanId = "";
        configId =  "3249E3F6-3B33-4D4E-93EB-2F464AB424A8";
        configName = "ConfigName:3249E3F6+3B33+4D4E+93EB+2F464AB424A8";
        clientId =  "B54963EE-C60A-443D-B352-2B5BDAB8B2BC";
        clientName = "Rapid7:B54963EE+C60A+443D+B352+2B5BDAB8B2BC";
        mockHttpClient = mock(HttpClient.class);
        engineGroupDetails = new ArrayList<>();

        String firstClientId = UUID.randomUUID().toString();
        engineGroupDetails.add(new EngineStub(firstClientId));
        engineGroupDetails.add(new EngineStub(firstClientId));
        engineGroupDetails.add(new EngineStub(UUID.randomUUID().toString()));
        engineGroupDetails.add(new EngineStub(UUID.randomUUID().toString()));
    }

    @Override
    public void close() {
    }

    public String getConfigName() {
        return configName;
    }

    public String getConfigId() {
        return configId;
    }

    public String getExpectedScanId() {
        return expectedScanId;
    }

    public EnterpriseClient getEnterpriseClient() {
        return enterpriseClient;
    }

    public String getExpectedAuthToken() {
        return expectedAuthToken;
    }

    public ApiSerializer getMockApiSerializer() {
        return mockApiSerializer;
    }
    public LoggerFacade getMockLogger() {
        return mockLogger;
    }

    public ContentHelper getMockContentHelper() {
        return mockContentHelper;
    }
    public Map<String, String> getExpectedEngineGroupsIdsByName() {
        return expectedEngineGroupsIdsByName;
    }

    public Map<String, String> getExpectedEngineGroupsNamesForClient() {
        return expectedEngineGroupsNamesForClient;
    }
    public String getFirstEngineId() {
        return engineGroupDetails.get(0).getId();
    }
    public String getFirstEngineName() {
        return engineGroupDetails.get(0).getName();
    }
    public String[] getEngineGroupNames() {
        return toStringArray(engineGroupDetails.stream().map(EngineStub::getName).collect(Collectors.toList()));
    }


    public EnterpriseClientTestContext arrangeExpectedValues() {
        return arrangeExpectedValues(true);
    }
    public EnterpriseClientTestContext arrangeExpectedValues(boolean isSuccess) {
        if (isSuccess) {
            expectedAuthToken = UUID.randomUUID().toString();
            expectedScanId = UUID.randomUUID().toString();

            expectedEngineGroupsIdsByName = engineGroupDetails
                .stream()
                .collect(Collectors.toMap(EngineStub::getName, EngineStub::getId));
            expectedEngineGroupsNamesForClient = engineGroupDetails
                .stream()
                .collect(Collectors.toMap(EngineStub::getId, EngineStub::getName, (key1, key2) -> key1));

        } else {
            expectedAuthToken = "";
            expectedScanId = "";

            expectedEngineGroupsIdsByName = new HashMap<>();
            expectedEngineGroupsNamesForClient = new HashMap<>();
        }
        return this;
    }

    public EnterpriseClientTestContext configureEnterpriseClient() {
        enterpriseClient = new EnterpriseRestClient(new HttpClientService(mockHttpClient, mockContentHelper, mockLogger), url, mockApiSerializer, mockContentHelper, mockLogger);
        return this;
    }

    public EnterpriseClientTestContext configureLogin(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> loginRequest = request -> request instanceof HttpPost &&  request.getURI().toString().equals(url + AUTHENTICATION_LOGIN);
        HttpResponse response = getAuthenticationResult(isSuccess, expectedAuthToken);
        when(mockHttpClient.execute(argThat(loginRequest))).thenReturn(response);
        return this;
    }

    public EnterpriseClientTestContext configureGetAllEngineGroups(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getAllEngineGroupsRequest = request -> request instanceof HttpGet &&  request.getURI().toString().equals(url + GET_ALL_ENGINE_GROUPS);
        HttpResponse response = getEngineGroupsResponse(isSuccess, engineGroupDetails);
        when(mockHttpClient.execute(argThat(getAllEngineGroupsRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureGetEngineGroupsForClient(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getEngineGroupsForClientRequest = request -> request instanceof HttpGet &&  request.getURI().toString().equals(url + GET_ENGINE_GROUPS_FOR_CLIENT);
        HttpResponse response = getEngineGroupsResponse(isSuccess, engineGroupDetails);
        when(mockHttpClient.execute(argThat(getEngineGroupsForClientRequest))).thenReturn(response);
        return this;
    }

    public EnterpriseClientTestContext configureGetConfigs(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getConfigsRequest = request -> request instanceof HttpGet &&  request.getURI().toString().equals(url + GET_CONFIGS);
        HttpResponse response = getJsonArrayResponse(isSuccess, "Configs", getJsonFromEntries(getEntryFrom("Id", configId), getEntryFrom("Name", configName)));
        when(mockHttpClient.execute(argThat(getConfigsRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureRunScanByConfigId(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> loginRequest = request -> request instanceof HttpPost &&  request.getURI().toString().equals(url + RUN_SCAN);
        HttpResponse response = getRunScanByConfigIdResponse(isSuccess, expectedScanId);
        when(mockHttpClient.execute(argThat(loginRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureGetScanStatus(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getConfigsRequest = request -> request instanceof HttpGet &&  request.getURI().toString().startsWith(url + GET_SCAN_STATUS);
        HttpResponse response = getMockJsonResponseFromJSON(isSuccess, getJsonFromEntries(getEntryFrom("Status", isSuccess ? "Running" : "Not Running")));
        when(mockHttpClient.execute(argThat(getConfigsRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureIsScanFinished(boolean apiCallIsSuccess, boolean isSuccess, boolean resultIsSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getConfigsRequest = request -> request instanceof HttpGet &&  request.getURI().toString().startsWith(url + IS_SCAN_FINISHED);
        HttpResponse response = getMockJsonResponseFromJSON(apiCallIsSuccess, getJsonFromEntries(getEntryFrom("IsSuccess", isSuccess), getEntryFrom("Result", resultIsSuccess)));
        when(mockHttpClient.execute(argThat(getConfigsRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureHasReport(boolean apiCallIsSuccess, boolean isSuccess, boolean resultIsSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getConfigsRequest = request -> request instanceof HttpGet &&  request.getURI().toString().startsWith(url + HAS_REPORT);
        HttpResponse response = getMockJsonResponseFromJSON(apiCallIsSuccess, getJsonFromEntries(getEntryFrom("IsSuccess", isSuccess), getEntryFrom("Result", resultIsSuccess)));
        when(mockHttpClient.execute(argThat(getConfigsRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureSaveConfig(boolean apiCallIsSuccess, boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> saveConfigRequest = request -> request instanceof HttpPost &&  request.getURI().toString().startsWith(url + SAVE_CONFIG);
        HttpResponse response = getMockJsonResponseFromJSON(apiCallIsSuccess, getJsonFromEntries(getEntryFrom("IsSuccess", isSuccess)));
        when(mockHttpClient.execute(argThat(saveConfigRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureGetVulnerabilitiesSummaryXml(boolean apiCallIsSuccess, String xml) throws IOException {
        ArgumentMatcher<HttpRequestBase> getRequest = request -> request instanceof HttpGet &&  request.getURI().toString().startsWith(url + GET_VULNERABILITIES_SUMMARY);
        HttpResponse response = getMockResponseFromEntity(apiCallIsSuccess, new StringEntity(xml, ContentType.TEXT_XML));
        when(mockHttpClient.execute(argThat(getRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureGetReportZip(boolean apiCallIsSuccess, byte[] content) throws IOException {
        ArgumentMatcher<HttpRequestBase> getRequest = request -> request instanceof HttpGet &&  request.getURI().toString().startsWith(url + GET_REPORT_ZIP);
        HttpResponse response = getMockResponseFromEntity(apiCallIsSuccess, new ByteArrayEntity(content));
        when(mockHttpClient.execute(argThat(getRequest))).thenReturn(response);
        return this;
    }
    public EnterpriseClientTestContext configureGetClientIdNamePairs(boolean isSuccess) throws IOException {
        ArgumentMatcher<HttpRequestBase> getConfigsRequest = request -> request instanceof HttpGet &&  request.getURI().toString().equals(url + GET_CLIENTS);
        HttpResponse response = getJsonArrayResponseWithSuccess(isSuccess, "Clients", getJsonFromEntries(getEntryFrom("ClientId", clientId), getEntryFrom("ClientName", clientName)));
        when(mockHttpClient.execute(argThat(getConfigsRequest))).thenReturn(response);
        return this;
    }

    private static HttpResponse getAuthenticationResult(boolean isSuccess, String token) {
        return getMockJsonResponseFromEntries(isSuccess, getEntryFrom("IsSuccess", isSuccess ? "true" : "false"), getEntryFrom("Token", token));
    }
    @SafeVarargs
    private static HttpResponse getJsonArrayResponse(boolean isSuccess, String key, JSONObject... objects) {
        return getJsonArrayResponse(isSuccess, key, Arrays.asList(objects));
    }
    @SafeVarargs
    private static HttpResponse getJsonArrayResponseWithSuccess(boolean isSuccess, String key, JSONObject... objects) {
        return getJsonArrayResponseWithSuccess(isSuccess, key, Arrays.asList(objects));
    }
    private static HttpResponse getJsonArrayResponseWithSuccess(boolean isSuccess, String key, List<JSONObject> objects) {
        JSONArray jsonArray = new JSONArray();
        objects.forEach(jsonArray::put);
        return getMockJsonResponseFromEntries(
            isSuccess, 
            getEntryFrom(key, jsonArray), 
            getEntryFrom("IsSuccess", isSuccess));
    }
    private static HttpResponse getJsonArrayResponse(boolean isSuccess, String key, List<JSONObject> objects) {
        JSONArray jsonArray = new JSONArray();
        objects.forEach(jsonArray::put);
        return getMockJsonResponseFromEntries(isSuccess, getEntryFrom(key, jsonArray));
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
    private static HttpResponse getEngineGroupsResponse(boolean isSuccess, List<EngineStub> engineGroupDetails) {
        return getJsonArrayResponse(isSuccess, "EngineGroups", engineGroupDetails.stream().map(engineGroup -> {
            JSONObject jsonObject = new JSONObject();
            jsonObject.put("Name", engineGroup.getName());
            jsonObject.put("Id", engineGroup.getId());
            return jsonObject;
        }).collect(Collectors.toList()));
    }

}
