package com.rapid7.appspider;

import org.json.JSONObject;
import org.json.JSONArray;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nbugash on 09/07/15.
 */
public class ScanManagement extends Base {

    public final static String GETSCANS = "/Scan/GetScans";
    public final static String RUNSCAN = "/Scan/RunScan";
    public final static String CANCELSCAN = "/Scan/CancelScan";
    public final static String PAUSESCAN = "/Scan/PauseScan";
    public final static String RESUMESCAN = "/Scan/ResumeScan";
    public final static String PAUSEALLSCANS = "/Scan/PauseAllScans";
    public final static String STOPALLSCANS = "/Scan/StopAllScans";
    public final static String RESUMEALLSCANS = "/Scan/ResumeAllScans";
    public final static String CANCELALLSCANS = "/Scan/CancelAllScans";
    public final static String GETSCANSTATUS = "/Scan/GetScanStatus";
    public final static String ISSCANACTIVE = "/Scan/IsScanActive";
    public final static String ISSCANFINISHED = "/Scan/IsScanFinished";
    public final static String HASREPORT = "/Scan/HasReport";
    public final static String GETSCANERRORS = "/Scan/GetScanErrors";

    /**
     * @param restUrl
     * @param authToken
     * @return
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
     * @return
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
     * @return
     */
    public static JSONObject runScanByConfigName(String restUrl, String authToken, String configName) {

        JSONObject jsonResponse = ScanConfiguration.getConfigs(restUrl, authToken);
        JSONArray allConfigs = (JSONArray) jsonResponse.get("Configs");

        // Iterate over the JSONArray until
        JSONObject config;
        int i = 0;
        do {
            config = allConfigs.getJSONObject(i);
            i++;
        } while (!config.get("Name").equals(configName));

        if (config.get("Name").equals(configName)) {
            String apiCall = restUrl + RUNSCAN;
            Map<String, String> params = new HashMap<String, String>();
            params.put("configId", (String) config.get("Id"));
            Object response = post(apiCall, authToken, params);
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
     * @return
     */
    public static JSONObject getScanStatus(String restUrl, String authToken, String scanId) {
        String apiCall = restUrl + GETSCANSTATUS;
        Map<String, String> params = new HashMap<String, String>();
        params.put("scanId", scanId);
        Object response = get(apiCall, authToken, params);
        if (response.getClass().equals(JSONObject.class)) {
            return (JSONObject) response;
        }
        return null;
    }

    /**
     * @param restUrl
     * @param authToken
     * @param scanId
     * @return
     */
    public static JSONObject hasReport(String restUrl, String authToken, String scanId) {
        String apiCall = restUrl + HASREPORT;
        Map<String, String> params = new HashMap<String, String>();
        params.put("scanId", scanId);
        Object response = get(apiCall, authToken, params);
        if (response.getClass().equals(JSONObject.class)) {
            return (JSONObject) response;
        }
        return null;
    }

}
