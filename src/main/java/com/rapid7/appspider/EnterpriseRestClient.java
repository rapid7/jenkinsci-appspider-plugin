/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ClientIdNamePair;
import com.rapid7.appspider.datatransferobjects.ScanResult;
import com.rapid7.appspider.models.AuthenticationModel;

import freemarker.template.Template;
import freemarker.template.TemplateException;

import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.jfree.util.Log;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Provides methods to communicating with AppSpider Enterprise while obsuring the implementation
 * details of that communication.
 */
public final class EnterpriseRestClient implements EnterpriseClient {

    private final String restEndPointUrl;
    private final HttpClientService clientService;
    private final LoggerFacade logger;
    private final ApiSerializer apiSerializer;
    private final ContentHelper contentHelper;

    /**
     * Instantiates a new instance of the EnterpriseClient class
     * @param clientService helper that works directly with lower level HttpClient methods
     * @param restEndPointUrl base endpoint including /rest/v1 or equilvalent path
     * @param apiSerializer Helper class providing handling of HttpResponse to JSONObject methods
     * @param contentHelper Helper class providing parsing and encoding methods to support api calls
     * @param logger logger used for diagnostic output
     * @throws IllegalArgumentException thrown if any of the arguments are null or if restEntPointUrl is empty
     */
    public EnterpriseRestClient(HttpClientService clientService, String restEndPointUrl, ApiSerializer apiSerializer, ContentHelper contentHelper, LoggerFacade logger) {

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
     * {@inheritDoc}
     */
    @Override
    public String getUrl() {
        return restEndPointUrl;
    }

    private static final String AUTHENTICATION_LOGIN = "/Authentication/Login";
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> login(AuthenticationModel authModel) {
        final String endPoint = restEndPointUrl + AUTHENTICATION_LOGIN;

        if (authModel == null) {
            throw new IllegalArgumentException();
        }

        Log.info("name: " + authModel.getUsername());
        Log.info("pass: " + authModel.getPassword());
        Log.info("had id: " + authModel.hasClientId());

        try {
            final StringEntity contentEntity = authModel.hasClientId()
                ? contentHelper.entityFrom(
                    contentHelper.pairFrom("name", authModel.getUsername()), 
                    contentHelper.pairFrom("password", authModel.getPassword()),
                    contentHelper.pairFrom("clientId", authModel.getClientId()))
                : contentHelper.entityFrom(
                    contentHelper.pairFrom("name", authModel.getUsername()), 
                    contentHelper.pairFrom("password", authModel.getPassword()));
            return clientService
                .buildPostRequestUsingApplicationJson(endPoint, contentEntity)
                .flatMap(request -> clientService.executeJsonRequest(request)
                .flatMap(apiSerializer::getTokenOrEmpty));
        } catch (UnsupportedEncodingException | JSONException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean testAuthentication(AuthenticationModel authModel) {
        return login(authModel).isPresent();
    }

    // <editor-fold desc="Engine Group APIs">

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String[]> getEngineGroupNamesForClient(String authToken) {
        return getEngineGroupsForClient(authToken)
                .map(map -> new ArrayList<>(map.keySet()))
                .map(Utility::toStringArray);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getEngineGroupIdFromName(String authToken, String engineGroupName) {
        return getAllEngineGroups(authToken).filter(map -> map.containsKey(engineGroupName)).flatMap(map -> Optional.of(map.get(engineGroupName)));
    }

    private static final String GET_ALL_ENGINE_GROUPS = "/EngineGroup/GetAllEngineGroups";
    private static final String GET_ENGINE_GROUPS_FOR_CLIENT = "/EngineGroup/GetEngineGroupsForClient";
    private Optional<Map<String, String>> getAllEngineGroups(String authToken) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_ALL_ENGINE_GROUPS, authToken)
            .flatMap(get -> contentHelper.asMapOfStringToString("Name", "Id", clientService.executeJsonRequest(get)));
    }
    private Optional<Map<String,String>> getEngineGroupsForClient(String authToken) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_ENGINE_GROUPS_FOR_CLIENT, authToken)
            .flatMap(get -> contentHelper.asMapOfStringToString("Name", "Id", clientService.executeJsonRequest(get)));
    }
    // </editor-fold>

    // <editor-fold desc="Scan APIs">

    /**
     * {@inheritDoc}
     */
    @Override
    public ScanResult runScanByConfigName(String authToken, String configName) {
        return getConfigByName(authToken, configName)
            .flatMap(apiSerializer::getScanConfigId)
            .map(configId -> runScanByConfigId(authToken, configId))
            .orElse(new ScanResult(false, ""));
    }

    private static final String GET_SCAN_STATUS = "/Scan/GetScanStatus";
    private static final String SCAN_ID = "scanId";
    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getScanStatus(String authToken, String scanId) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_SCAN_STATUS, authToken, contentHelper.pairFrom(SCAN_ID, scanId))
            .flatMap(clientService::executeJsonRequest)
            .flatMap(apiSerializer::getStatus);
    }

    private static final String IS_SCAN_FINISHED = "/Scan/IsScanFinished";

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScanFinished(String authToken, String scanId) {
        return resultAndIsSuccessProvider(restEndPointUrl + IS_SCAN_FINISHED, authToken, scanId);
    }

    private static final String HAS_REPORT = "/Scan/HasReport";
    /**
     * {@inheritDoc}
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
            .buildGetRequestUsingFormUrlEncoding(endpoint, authToken, contentHelper.pairFrom(SCAN_ID, scanId))
            .flatMap(clientService::executeJsonRequest)
            .map(apiSerializer::getResultIsSuccess)
            .orElse(false);
    }

    // </editor-fold>

    // <editor-fold desc="Config APIs">
    private static final String GET_CONFIGS = "/Config/GetConfigs";
    private static final String SAVE_CONFIG = "/Config/SaveConfig";

    /**
     * calls the /Configs/SaveConfig endpoint using the provided data to create or update a configuration
     * @param authToken authorization token required to execute request
     * @param name name of the scanconfig to save
     * @param url target URL for the scan
     * @param engineGroupId unique engine group id for the engine(s) to be used to execute the scan
     * @return true on success; otherwise, false
     */
    private Optional<JSONObject> getConfigByName(String authToken, String configName) {
        return getConfigs(authToken)
            .flatMap(configs -> apiSerializer.findByConfigName(configs, configName));
    }

    /**
     * calls the /Config/GetConfigs endpoint returning the resulting JSONArray of Configs on success
     * @param authToken authorization token required to execute request
     * @return Optional containing JSONArray on success; otherwise Optional.empty()
     */
    private Optional<JSONArray> getConfigs(String authToken) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_CONFIGS, authToken)
            .flatMap(clientService::executeJsonRequest)
            .flatMap(configsObject -> contentHelper.getArrayFrom(configsObject, "Configs"));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String[]> getConfigNames(String authToken) {
        return getConfigs(authToken)
            .flatMap(apiSerializer::getConfigNames)
            .map(Utility::toStringArray);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean saveConfig(String authToken, String name, URL url, String engineGroupId) {

        try {
            Template template = FreemarkerConfiguration
                .getInstance()
                .getTemplate("scanConfigTemplate.ftl");

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
    private static final String GET_VULNERABILITIES_SUMMARY = "/Report/GetVulnerabilitiesSummaryXml";
    private static final String GET_REPORT_ZIP = "/Report/GetReportZip";

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<String> getVulnerabilitiesSummaryXml(String authToken, String scanId) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_VULNERABILITIES_SUMMARY, authToken, contentHelper.pairFrom(SCAN_ID, scanId))
            .flatMap(clientService::executeEntityRequest)
            .flatMap(contentHelper::getTextHtmlOrXmlContent);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<InputStream> getReportZip(String authToken, String scanId) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_REPORT_ZIP, authToken, contentHelper.pairFrom(SCAN_ID, scanId))
            .flatMap(clientService::executeEntityRequest)
            .flatMap(contentHelper::getInputStream);
    }

    // </editor-fold>

    // <editor-fold desc="Config APIs">
    private static final String GET_CLIENTS = "/Config/GetClients";

    /**
     * {@inheritDoc}
     */
    public Optional<List<ClientIdNamePair>> getClientNameIdPairs(String authToken) {
        return clientService
            .buildGetRequestUsingFormUrlEncoding(restEndPointUrl + GET_CLIENTS, authToken)
            .flatMap(clientService::executeJsonRequest)
            .filter(apiSerializer::getIsSuccess)
            .flatMap(clientObjects -> contentHelper.getArrayFrom(clientObjects, "Clients"))
            .flatMap(apiSerializer::getClientIdNamePairs);
    }

    // </editor-fold>
}
