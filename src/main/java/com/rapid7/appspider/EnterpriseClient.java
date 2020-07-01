/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.http.message.BasicNameValuePair;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides methods to communicating with AppSpider Enterprise while obsuring the implementation
 * details of that communication.
 */
class EnterpriseClient {

    private final String restEndPointUrl;
    private final HttpClientService clientHelper;
    private final LoggerFacade logger;
    private final ApiSerializer apiSerializer;
    private final static String AUTHENTICATION_LOGIN = "/Authentication/Login";
    private JsonHelper jsonHelper;

    /**
     * Instantiates a new instance of the EnterpriseClient class
     * @param clientHelper helper that works directly with lower level HttpClient methods
     * @param restEndPointUrl base endpoint including /rest/v1 or equilvalent path
     * @param logger logger used for diagnostic output
     * @throws IllegalArgumentException thrown if any of the arguments are null or if restEntPointUrl is empty
     */
    public EnterpriseClient(HttpClientService clientHelper, String restEndPointUrl, ApiSerializer apiSerializer, JsonHelper jsonHelper, LoggerFacade logger) {

        if (Objects.isNull(restEndPointUrl) || restEndPointUrl.isEmpty())
            throw new IllegalArgumentException("restEndPointUrl cannot be null or empty");
        if (Objects.isNull(clientHelper))
            throw new IllegalArgumentException("clientHelper cannot be null or empty");
        if (Objects.isNull(apiSerializer))
            throw new IllegalArgumentException("apiSerializer cannot be null or empty");
        if (Objects.isNull(jsonHelper))
            throw new IllegalArgumentException("jsonHelper cannot be null");
        if (Objects.isNull(logger))
            throw new IllegalArgumentException("logger cannot be null");

        this.restEndPointUrl = restEndPointUrl;
        this.clientHelper = clientHelper;
        this.apiSerializer = apiSerializer;
        this.jsonHelper = jsonHelper;
        this.logger = logger;
    }

    /**
     * calls the /Authentication/Login endpoint with provided details
     * @param username Username
     * @param password Password
     * @return on success Optional containing the authorization token; otherwise empty
     */
    public Optional<String> login(String username, String password) {
        String endPoint = restEndPointUrl + AUTHENTICATION_LOGIN;

        if (Objects.isNull(username) || username.isEmpty())
            throw new IllegalArgumentException("username cannot be null or empty");
        if (Objects.isNull(password) || password.isEmpty())
            throw new IllegalArgumentException("password cannot be null or empty");

        try {
            return clientHelper
                .buildPostRequestUsingApplicationJson(
                    endPoint,
                    jsonHelper.entityFrom(
                            jsonHelper.pairFrom("name", username),
                            jsonHelper.pairFrom("password", password)))
                .flatMap(request -> clientHelper.executeRequest(request)
                    .flatMap(apiSerializer::getTokenOrEmpty));
        } catch (UnsupportedEncodingException | JSONException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    // <editor-fold desc="Engine Group APIs">
    private final static String GET_ALL_ENGINE_GROUPS = "/EngineGroup/GetAllEngineGroups";
    private final static String GET_ENGINE_GROUPS_FOR_CLIENT = "/EngineGroup/GetEngineGroupsForClient";

    public Optional<Map<String, String>> getAllEngineGroups(String authToken) {
        return clientHelper
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_ALL_ENGINE_GROUPS, authToken)
            .flatMap(get -> jsonHelper.asMapOfStringToString(clientHelper.executeRequest(get)));
    }
    public Optional<Map<String,String>> getEngineGroupsForClient(String authToken) {
        return clientHelper
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_ENGINE_GROUPS_FOR_CLIENT, authToken)
            .flatMap(get -> jsonHelper.asMapOfStringToString(clientHelper.executeRequest(get)));
    }
    public Optional<String[]> getEngineNameGroupForClient(String authToken) {
        return getEngineGroupsForClient(authToken)
                .map(map -> new ArrayList<>(map.keySet()))
                .map(Utility::toStringArray);
    }
    public Optional<String> getEngineGroupIdFromName(String authToken, String engineGroupName) {
        return getAllEngineGroups(authToken).flatMap(map -> Optional.of(map.get(engineGroupName)));
    }
    // </editor-fold>

    // <editor-fold desc="Scan APIs">
    private final static String RUN_SCAN = "/Scan/RunScan";

    public boolean runScanByConfigId(String authToken, String configId) {
        return clientHelper
            .buildPostRequestUsingFormUrlEncoding(
                    restEndPointUrl + RUN_SCAN,
                    authToken,
                    new BasicNameValuePair("configId", configId))
            .flatMap(clientHelper::executeRequest)
            .map(apiSerializer::getIsSuccess)
            .orElse(false);
    }

    public boolean runScanByConfigName(String authToken, String configName) {
        return getConfigByName(authToken, configName)
            .flatMap(apiSerializer::getScanConfigId)
            .map(configId -> runScanByConfigId(authToken, configId))
            .orElse(false);
    }

    public Optional<String> getScanStatus(String authToken, String scanId) {
        return Optional.empty();
    }
    public Optional<Boolean> isScanFinished(String authToken, String scanId) {
        return Optional.empty();
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
    public Optional<JSONObject> getConfigByName(String authToken, String configName) {
        return getConfigs(authToken)
            .flatMap(configs -> apiSerializer.findByConfigName(configs, configName));
    }

    /**
     * calls the /Config/GetConfigs endpoint returning the resulting JSONArray of Configs on success
     * @param authToken authorization token required to execute request
     * @return Optional containing JSONArray on success; otherwise Optional.empty()
     */
    public Optional<JSONArray> getConfigs(String authToken) {
        return clientHelper
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_CONFIGS, authToken)
            .flatMap(clientHelper::executeRequest)
            .flatMap(configsObject -> jsonHelper.getArrayFrom(configsObject, "Configs"));
    }

    /**
     * returns String[] of scan config names
     * @param authToken authorization token required to execute request
     * @return String[] of all scan config names
     */
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
    public boolean saveConfig(String authToken, String name, String url, String engineGroupId) {

        try {
            Template template = new Configuration().getTemplate(
                    "src/main/java/com/rapid7/appspider/template/scanConfigTemplate.ftl");

            String scanConfigXml = apiSerializer.getScanConfigXml(template, name, url);
            return clientHelper
                    .buildPostRequestUsingFormUrlEncoding(
                            restEndPointUrl + SAVE_CONFIG,
                            authToken,
                            jsonHelper.pairFrom("defendEnabled", "true"),
                            jsonHelper.pairFrom("monitoringDelay", "0"),
                            jsonHelper.pairFrom("monitoringTriggerScan", "true"),
                            jsonHelper.pairFrom("id", "null"),
                            jsonHelper.pairFrom("name",name),
                            jsonHelper.pairFrom("clientId", "null"),
                            jsonHelper.pairFrom("engineGroupId",engineGroupId),
                            jsonHelper.pairFrom("monitoring", "true"),
                            jsonHelper.pairFrom("isApproveRequired", "false"),
                            jsonHelper.pairFrom("scanconfigxml", scanConfigXml))
                    .flatMap(clientHelper::executeRequest)
                    .map(apiSerializer::getIsSuccess)
                    .orElse(false);

        } catch (IOException | TemplateException e) {
            logger.println(e.toString());
            return false;
        }
    }

    // </editor-fold>
}
