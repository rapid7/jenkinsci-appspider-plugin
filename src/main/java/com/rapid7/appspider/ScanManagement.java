package com.rapid7.appspider;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nbugash on 09/07/15.
 */
public class ScanManagement extends Base {

    private final static String GETSCANS = "/Scan/GetScans";
    private final static String RUNSCAN = "/Scan/RunScan";
    private final static String CANCELSCAN = "/Scan/CancelScan";
    private final static String PAUSESCAN = "/Scan/PauseScan";
    private final static String RESUMESCAN = "/Scan/ResumeScan";
    private final static String PAUSEALLSCANS = "/Scan/PauseAllScans";
    private final static String STOPALLSCANS = "/Scan/StopAllScans";
    private final static String RESUMEALLSCANS = "/Scan/ResumeAllScans";
    private final static String CANCELALLSCANS = "/Scan/CancelAllScans";
    private final static String GETSCANSTATUS = "/Scan/GetScanStatus";
    private final static String ISSCANACTIVE = "/Scan/IsScanActive";
    private final static String ISSCANFINISHED = "/Scan/IsScanFinished";
    private final static String HASREPORT = "/Scan/HasReport";
    private final static String GETSCANERRORS = "/Scan/GetScanErrors";

    /**
     * @param restUrl
     * @param authToken
     * @return JSONObject response body of getScans api call
     */
    public static JSONObject getScans(String restUrl, String authToken) {
        String apiCall = restUrl + GETSCANS;
        Object response = get(apiCall, authToken);
        if (response.getClass().equals(JSONObject.class)) {
            return (JSONObject) response;
        }
        return null;
    }

    /**
     * @param restUrl
     * @param authToken
     * @param configId
     * @return JSONObject response body of ScanByConfigId api call
     */
    public static JSONObject runScanByConfigId(String restUrl, String authToken, String configId) {
        String apiCall = restUrl + RUNSCAN;
        Map<String, String> params = new HashMap<String, String>();
        params.put("configId", configId);
        Object response = post(apiCall, authToken, params);
        if (response.getClass().equals(JSONObject.class)) {
            return (JSONObject) response;
        }
        return null;
    }

    /**
     * @param restUrl
     * @param authToken
     * @param configName
     * @return JSONObject response body of scans by config name api call
     */
    public static JSONObject runScanByConfigName(String restUrl, String authToken, String configName) {

        JSONObject jsonResponse = ScanConfiguration.getConfigs(restUrl, authToken);
        JSONArray allConfigs = jsonResponse.getJSONArray("Configs");

        // Iterate over the JSONArray until
        JSONObject config;
        int i = 0;
        do {
            config = allConfigs.getJSONObject(i);
            i++;
        } while ((!config.getString("Name").equalsIgnoreCase(configName)) && i < allConfigs.length());

        if (config.getString("Name").equalsIgnoreCase(configName)) {
            Object response = runScanByConfigId(restUrl, authToken, config.getString("Id"));
            if (response.getClass().equals(JSONObject.class)) {
                return (JSONObject) response;
            }
        } else {
            throw new RuntimeException("Config name " + configName + " does not exist!!");
        }
        return null;
    }

    /**
     * @param restUrl
     * @param authToken
     * @param scanId
     * @return String containing current scan status on success; otherwise, null
     */
    public static String getScanStatus(String restUrl, String authToken, String scanId) {
        String apiCall = restUrl + GETSCANSTATUS;
        Map<String, String> params = new HashMap<String, String>();
        params.put("scanId", scanId);
        Object response = get(apiCall, authToken, params);
        if (response.getClass().equals(JSONObject.class)) {
            return ((JSONObject)response).getString("Status");
        }
        return null;
    }

    /**
     * @param restUrl
     * @param authToken
     * @param scanId
     * @return  Boolean on success representing whether the report is present or not on success, or null on failure
     */
    public static Boolean hasReport(String restUrl, String authToken, String scanId) {
        String apiCall = restUrl + HASREPORT;
        Map<String, String> params = new HashMap<String, String>();
        params.put("scanId", scanId);
        Object response = get(apiCall, authToken, params);
        if (response.getClass().equals(JSONObject.class)) {
            return ((JSONObject)response).getBoolean("Result")&&
                    ((JSONObject) response).getBoolean("IsSuccess");
        }
        return null;
    }

    public static Boolean isScanFinished(String restUrl, String authToken, String scanId) {
        String apiCall = restUrl + ISSCANFINISHED;
        Map<String,String> params = new HashMap<String, String>();
        params.put("scanId", scanId);
        Object response = get(apiCall,authToken,params);
        if (response.getClass().equals(JSONObject.class)) {
            return ((JSONObject)response).getBoolean("Result") &&
                   ((JSONObject) response).getBoolean("IsSuccess");
        }
        return null;
    }

}
