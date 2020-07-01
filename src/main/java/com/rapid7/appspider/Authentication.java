package com.rapid7.appspider;

import org.json.JSONObject;

import java.io.PrintStream;

/**
 * Created by nbugash on 08/07/15.
 */
public class Authentication extends Base {

    private final static String API = "/Authentication/Login";

    /**
     * @param restUrl Restful api url of the Appspider enterprise
     * @param username Username to log in to the Appspider enterprise
     * @param password Password
     * @return the authorization token
     */
    public static String authenticate(String restUrl, String username, String password) {
        String apiCall = restUrl + API;
        Object response = post(apiCall, username, password);
        if (response == null || !(response.getClass().equals(JSONObject.class))) {
            return "";
        }
        JSONObject jsonObject = (JSONObject)response;
        if (jsonObject.getBoolean("IsSuccess")) {
            return jsonObject.getString("Token");
        } else {
            return "";
        }
    }
}
