/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ScanResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.Map;
import java.util.Optional;

public interface EnterpriseClient {
    /**
     * returns the full URL for the enterprise rest endpoint
     * @return the full URL for the enterprise rest endpoint
     */
    String getUrl();

    /**
     * calls the /Authentication/Login endpoint with provided details
     * @param username Username
     * @param password Password
     * @return on success Optional containing the authorization token; otherwise empty
     */
    Optional<String> login(String username, String password);

    /**
     * calls the /Authentication/Login endpoint with provided details returning true if credentials are valid
     * @param username Username
     * @param password Password
     * @return true if endpoint returns authorization token; otherwise, false
     */
    boolean testAuthentication(String username, String password);

    /**
     * fetches the names of available engine groups
     * @param authToken authorization token required to execute request
     * @return On success an Optional containing an array of Strings
     *         representing the names of available engine groups;
     *         otherwise, Optional.empty()
     */
    Optional<String[]> getEngineNamesGroupForClient(String authToken);

    /**
     * fetches the unique id of the engine group given by engineGroupName
     * @param authToken authorization token required to execute request
     * @param engineGroupName name of the engine to get the id of
     * @return Optional containing the id of the engine group if found;
     *         otherwise, Optional.empty()
     */
    Optional<String> getEngineGroupIdFromName(String authToken, String engineGroupName);

    /**
     * starts a new scan using configuration matching configName
     * @param authToken authorization token required to execute request
     * @param configName name of the config to run
     * @return ScanResult containing details on the success of the request and if successful the
     *         unique id of the scan
     */
    ScanResult runScanByConfigName(String authToken, String configName);

    /**
     * gets the current status of the scan identified by scanId
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan
     * @return Optional containing current scan status as String on success; Otherwise Optional.empty()
     */
    Optional<String> getScanStatus(String authToken, String scanId);

    /**
     * determines if the scan identified by scanId has finished
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan
     * @return true if scan has finished regardless of how it finished, or false if it hasn't
     */
    boolean isScanFinished(String authToken, String scanId);

    /**
     * determines if a scan identified by scanId has a report or not
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan
     * @return true if the scan has a report; otherwise, false
     */
    boolean hasReport(String authToken, String scanId);

    /**
     * calls /Config/GetConfigs endpoint and returns the JSONObject from those results matching configName
     * @param authToken authorization token required to execute request
     * @param configName name of the matching configuration to return
     * @return Optional containing the JSONObject of the matching configuration on success;
     *         otherwise, Optional.empty()
     */
    Optional<JSONObject> getConfigByName(String authToken, String configName);

    /**
     * calls the /Config/GetConfigs endpoint returning the resulting JSONArray of Configs on success
     * @param authToken authorization token required to execute request
     * @return Optional containing JSONArray on success; otherwise Optional.empty()
     */
    Optional<JSONArray> getConfigs(String authToken);

    /**
     * returns String[] of scan config names
     * @param authToken authorization token required to execute request
     * @return String[] of all scan config names
     */
    Optional<String[]> getConfigNames(String authToken);

    /**
     * calls the /Configs/SaveConfig endpoint using the provided data to create or update a configuration
     * @param authToken authorization token required to execute request
     * @param name name of the scanconfig to save
     * @param url target URL for the scan
     * @param engineGroupId unique engine group id for the engine(s) to be used to execute the scan
     * @return true on success; otherwise, false
     */
    boolean saveConfig(String authToken, String name, String url, String engineGroupId);

    /**
     * gets the vulnerability summary XML as a String
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan to provide report for
     * @return Optional containing the vulnerability summary as XML String on success;
     *         otherwise, Optional.empty()
     */
    Optional<String> getVulnerabilitiesSummaryXml(String authToken, String scanId);

    /**
     * provides InputStream for the request report zip
     * @param authToken authorization token required to execute request
     * @param scanId unique scan identifier of the scan to provide report for
     * @return Optional containing InputStream on success; otherwise, Optional.empty()
     */
    Optional<InputStream> getReportZip(String authToken, String scanId);
}
