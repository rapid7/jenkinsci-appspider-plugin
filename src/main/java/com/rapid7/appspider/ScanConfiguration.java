package com.rapid7.appspider;

import org.json.JSONObject;

/**
 * Created by nbugash on 08/07/15.
 */
public class ScanConfiguration extends Base {

    private static final String SAVECONFIG = "/Config/SaveConfig";
    private static final String DELETECONFIG = "/Config/DeleteConfig";
    private static final String GETCONFIGS = "/Config/GetConfigs";
    private static final String GETSCANCONFIG = "/Config/GetScanConfig";
    private static final String GETATTACHMENT = "/Config/GetAttachment";
    private static final String GETATTACHMENTS = "/Config/GetAttachments";

    public static JSONObject getConfigs(String restUrl, String authToken) {
        String apiCall = restUrl + GETCONFIGS;
        Object response = get(apiCall, authToken);
        if (response.getClass().equals(JSONObject.class)) {
            return (JSONObject) response;
        }
        return null;
    }
}
