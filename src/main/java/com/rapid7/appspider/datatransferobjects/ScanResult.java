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
    public ScanResult(JSONObject jsonObject) {
        if (jsonObject == null)
            throw new IllegalArgumentException("jsonObject cannot be null");

        try {
            isSuccess = jsonObject.getBoolean("IsSuccess");
            scanId = jsonObject.getJSONObject("Scan").getString("Id");

        } catch(JSONException e) {
            throw new IllegalArgumentException("unexpected error occured parsing scan result", e);
        }

    }

    public String getScanId() {
        return scanId;
    }

    public boolean isSuccess() {
        return isSuccess;
    }
}
