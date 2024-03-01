package com.rapid7.appspider.datatransferobjects;

import org.json.JSONException;
import org.json.JSONObject;

public class ScanResult {
    private final boolean isSuccess;
    private final String scanId;

    public ScanResult(boolean isSuccess, String scanId) {
        this.isSuccess = isSuccess;
        this.scanId = scanId;
    }

    public static ScanResult createInstanceFromJsonOrThrow(JSONObject jsonObject) {
        if (jsonObject == null)
            throw new IllegalArgumentException("jsonObject cannot be null");

        try {
            boolean isSuccess = jsonObject.getBoolean("IsSuccess");
            String scanId = jsonObject.getJSONObject("Scan").getString("Id");
            return new ScanResult(isSuccess, scanId);
        } catch(JSONException e) {
            throw new IllegalArgumentException("unexpected error occurred parsing scan result", e);
        }
    }

    public String getScanId() {
        return scanId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
