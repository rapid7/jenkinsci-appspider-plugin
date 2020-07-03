/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ScanResult;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides methods to communicating with AppSpider Enterprise while obsuring the implementation
 * details of that communication.
 */
public final class StandardEnterpriseClient implements EnterpriseClient {

    private final String restEndPointUrl;
    private final HttpClientService clientService;
    private final LoggerFacade logger;
    private final ApiSerializer apiSerializer;
    private final ContentHelper contentHelper;

    /**
     * Instantiates a new instance of the EnterpriseClient class
     * @param clientService helper that works directly with lower level HttpClient methods
     * @param restEndPointUrl base endpoint including /rest/v1 or equilvalent path
     * @param logger logger used for diagnostic output
     * @throws IllegalArgumentException thrown if any of the arguments are null or if restEntPointUrl is empty
     */
    public StandardEnterpriseClient(HttpClientService clientService, String restEndPointUrl, ApiSerializer apiSerializer, ContentHelper contentHelper, LoggerFacade logger) {

        if (Objects.isNull(restEndPointUrl) || restEndPointUrl.isEmpty())
            throw new IllegalArgumentException("restEndPointUrl cannot be null or empty");
        if (Objects.isNull(clientService))
            throw new IllegalArgumentException("clientHelper cannot be null or empty");
        if (Objects.isNull(apiSerializer))
            throw new IllegalArgumentException("apiSerializer cannot be null or empty");
        if (Objects.isNull(contentHelper))
            throw new IllegalArgumentException("jsonHelper cannot be null");
        if (Objects.isNull(logger))
            throw new IllegalArgumentException("logger cannot be null");

        this.restEndPointUrl = restEndPointUrl;
        this.clientService = clientService;
        this.apiSerializer = apiSerializer;
        this.contentHelper = contentHelper;
        this.logger = logger;
    }


    /**
     * returns the full URL for the enterprise rest endpoint
     * @return the full URL for the enterprise rest endpoint
     */
    @Override
    public String getUrl() {
        return restEndPointUrl;
    }

    private static final String AUTHENTICATION_LOGIN = "/Authentication/Login";
    /**
     * calls the /Authentication/Login endpoint with provided details
     * @param username Username
     * @param password Password
     * @return on success Optional containing the authorization token; otherwise empty
     */
    @Override
    public Optional<String> login(String username, String password) {
        String endPoint = restEndPointUrl + AUTHENTICATION_LOGIN;

        if (Objects.isNull(username) || username.isEmpty())
            throw new IllegalArgumentException("username cannot be null or empty");
        if (Objects.isNull(password) || password.isEmpty())
            throw new IllegalArgumentException("password cannot be null or empty");

        try {
            return clientService
                .buildPostRequestUsingApplicationJson(
                    endPoint,
                    contentHelper.entityFrom(
                            contentHelper.pairFrom("name", username),
                            contentHelper.pairFrom("password", password)))
                .flatMap(request -> clientService.executeJsonRequest(request)
                .flatMap(apiSerializer::getTokenOrEmpty));
        } catch (UnsupportedEncodingException | JSONException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    /**
     * calls the /Authentication/Login endpoint with provided details returning true if credentials are valid
     * @param username Username
     * @param password Password
     * @return true if endpoint returns authorization token; otherwise, false
     */
    @Override
    public boolean testAuthentication(String username, String password) {
        return login(username, password).isPresent();
    }

    // <editor-fold desc="Engine Group APIs">

    /**
     * fetches the names of available engine groups
     * @param authToken authorization token required to execute request
     * @return On success an Optional containing an array of Strings
     *         representing the names of available engine groups;
     *         otherwise, Optional.empty()
     */
    @Override
    public Optional<String[]> getEngineNamesGroupForClient(String authToken) {
        return getEngineGroupsForClient(authToken)
                .map(map -> new ArrayList<>(map.keySet()))
                .map(Utility::toStringArray);
    }

    /**
     * fetches the unique id of the engine group given by engineGroupName
     * @param authToken authorization token required to execute request
     * @param engineGroupName name of the engine to get the id of
     * @return Optional containing the id of the engine group if found;
     *         otherwise, Optional.empty()
     */
    @Override
    public Optional<String> getEngineGroupIdFromName(String authToken, String engineGroupName) {
        return getAllEngineGroups(authToken).flatMap(map -> Optional.of(map.get(engineGroupName)));
    }

    private static final String GET_ALL_ENGINE_GROUPS = "/EngineGroup/GetAllEngineGroups";
    private static final String GET_ENGINE_GROUPS_FOR_CLIENT = "/EngineGroup/GetEngineGroupsForClient";
    private Optional<Map<String, String>> getAllEngineGroups(String authToken) {
        return clientService
                .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_ALL_ENGINE_GROUPS, authToken)
                .flatMap(get -> contentHelper.asIdToNameMapOfStringToString(clientService.executeJsonRequest(get)));
    }
    private Optional<Map<String,String>> getEngineGroupsForClient(String authToken) {
        return clientService
                .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_ENGINE_GROUPS_FOR_CLIENT, authToken)
                .flatMap(get -> contentHelper.asIdToNameMapOfStringToString(clientService.executeJsonRequest(get)));
    }
    // </editor-fold>

    // <editor-fold desc="Scan APIs">

    /**
     * starts a new scan using configuration matching configName
     * @param authToken authorization token required to execute request
     * @param configName name of the config to run
     * @return ScanResult containing details on the success of the request and if successful the
     *         unique id of the scan
     */
    @Override
    public ScanResult runScanByConfigName(String authToken, String configName) {
        return getConfigByName(authToken, configName)
            .flatMap(apiSerializer::getScanConfigId)
            .map(configId -> runScanByConfigId(authToken, configId))
            .orElse(new ScanResult(false, ""));
    }

    private static final String GET_SCAN_STATUS = "/Scan/GetScanStatus";
    /**
     * gets the current status of the scan identified by scanId
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan
     * @return Optional containing current scan status as String on success; Otherwise Optional.empty()
     */
    @Override
    public Optional<String> getScanStatus(String authToken, String scanId) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_SCAN_STATUS, authToken, contentHelper.pairFrom("scanId", scanId))
            .flatMap(clientService::executeJsonRequest)
            .flatMap(apiSerializer::getStatus);
    }

    private static final String IS_SCAN_FINISHED = "/Scan/IsScanFinished";

    /**
     * determines if the scan identified by scanId has finished
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan
     * @return true if scan has finished regardless of how it finished, or false if it hasn't
     */
    @Override
    public boolean isScanFinished(String authToken, String scanId) {
        return resultAndIsSuccessProvider(restEndPointUrl + IS_SCAN_FINISHED, authToken, scanId);
    }

    private static final String HAS_REPORT = "/Scan/HasReport";
    /**
     * determines if a scan identified by scanId has a report or not
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan
     * @return true if the scan has a report; otherwise, false
     */
    @Override
    public boolean hasReport(String authToken, String scanId) {
        return resultAndIsSuccessProvider(restEndPointUrl + HAS_REPORT, authToken, scanId);
    }

    private static final String RUN_SCAN = "/Scan/RunScan";
    /**
     * starts a new scan using configuration matching configId
     * @param authToken authorization token required to execute request
     * @param configId id of the config to run
     * @return ScanResult containing details on the success of the request and if successful the
     *         unique id of the scan
     */
    private ScanResult runScanByConfigId(String authToken, String configId) {
        return clientService
                .buildPostRequestUsingFormUrlEncoding(
                        restEndPointUrl + RUN_SCAN,
                        authToken,
                        new BasicNameValuePair("configId", configId))
                .flatMap(clientService::executeJsonRequest)
                .map(apiSerializer::getScanResult)
                .orElse(new ScanResult(false, ""));
    }

    private boolean resultAndIsSuccessProvider(String endpoint, String authToken, String scanId) {
        return clientService
                .buildGetRequestUsingFormUrlEncoding(endpoint, authToken, contentHelper.pairFrom("scanId", scanId))
                .flatMap(clientService::executeJsonRequest)
                .map(apiSerializer::getResultIsSuccess)
                .orElse(false);
    }

    // </editor-fold>

    // <editor-fold desc="Config APIs">
    private static final String GET_CONFIGS = "/Config/GetConfigs";
    private static final String SAVE_CONFIG = "/Config/SaveConfig";

    /**
     * calls /Config/GetConfigs endpoint and returns the JSONObject from those results matching configName
     * @param authToken authorization token required to execute request
     * @param configName name of the matching configuration to return
     * @return Optional containing the JSONObject of the matching configuration on success;
     *         otherwise, Optional.empty()
     */
    @Override
    public Optional<JSONObject> getConfigByName(String authToken, String configName) {
        return getConfigs(authToken)
            .flatMap(configs -> apiSerializer.findByConfigName(configs, configName));
    }

    /**
     * calls the /Config/GetConfigs endpoint returning the resulting JSONArray of Configs on success
     * @param authToken authorization token required to execute request
     * @return Optional containing JSONArray on success; otherwise Optional.empty()
     */
    @Override
    public Optional<JSONArray> getConfigs(String authToken) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_CONFIGS, authToken)
            .flatMap(clientService::executeJsonRequest)
            .flatMap(configsObject -> contentHelper.getArrayFrom(configsObject, "Configs"));
    }

    /**
     * returns String[] of scan config names
     * @param authToken authorization token required to execute request
     * @return String[] of all scan config names
     */
    @Override
    public Optional<String[]> getConfigNames(String authToken) {
        return getConfigs(authToken)
            .flatMap(apiSerializer::getConfigNames)
            .map(Utility::toStringArray);
    }

    /**
     * calls the /Configs/SaveConfig endpoint using the provided data to create or update a configuration
     * @param authToken authorization token required to execute request
     * @param name name of the scanconfig to save
     * @param url target URL for the scan
     * @param engineGroupId unique engine group id for the engine(s) to be used to execute the scan
     * @return true on success; otherwise, false
     */
    @Override
    public boolean saveConfig(String authToken, String name, String url, String engineGroupId) {

        try {
            Template template = new Configuration().getTemplate(
                    "src/main/java/com/rapid7/appspider/template/scanConfigTemplate.ftl");

            String scanConfigXml = apiSerializer.getScanConfigXml(template, name, url);
            return clientService
                    .buildPostRequestUsingFormUrlEncoding(
                            restEndPointUrl + SAVE_CONFIG,
                            authToken,
                            contentHelper.pairFrom("defendEnabled", "true"),
                            contentHelper.pairFrom("monitoringDelay", "0"),
                            contentHelper.pairFrom("monitoringTriggerScan", "true"),
                            contentHelper.pairFrom("id", "null"),
                            contentHelper.pairFrom("name",name),
                            contentHelper.pairFrom("clientId", "null"),
                            contentHelper.pairFrom("engineGroupId",engineGroupId),
                            contentHelper.pairFrom("monitoring", "true"),
                            contentHelper.pairFrom("isApproveRequired", "false"),
                            contentHelper.pairFrom("scanconfigxml", scanConfigXml))
                    .flatMap(clientService::executeJsonRequest)
                    .map(apiSerializer::getIsSuccess)
                    .orElse(false);

        } catch (IOException | TemplateException e) {
            logger.println(e.toString());
            return false;
        }
    }

    // </editor-fold>

    // <editor-fold desc="Report APIs">
    private final static String GET_VULNERABILITIES_SUMMARY = "/Report/GetVulnerabilitiesSummaryXml";
    private final static String GET_REPORT_ZIP = "/Report/GetReportZip";

    /**
     * gets the vulnerability summary XML as a String
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan to provide report for
     * @return Optional containing the vulnerability summary as XML String on success;
     *         otherwise, Optional.empty()
     */
    @Override
    public Optional<String> getVulnerabilitiesSummaryXml(String authToken, String scanId) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_VULNERABILITIES_SUMMARY, authToken, contentHelper.pairFrom("scanId", scanId))
            .flatMap(clientService::executeEntityRequest)
            .flatMap(contentHelper::getTextHtmlOrXmlContent);
    }

    /**
     * provides InputStream for the request report zip
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan to provide report for
     * @return Optional containing InputStream on success; otherwise, Optional.empty()
     */
    @Override
    public Optional<InputStream> getReportZip(String authToken, String scanId) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_REPORT_ZIP, authToken, contentHelper.pairFrom("scanId", scanId))
            .flatMap(clientService::executeEntityRequest)
            .flatMap(contentHelper::getInputStream);
    }

    // </editor-fold>
}
