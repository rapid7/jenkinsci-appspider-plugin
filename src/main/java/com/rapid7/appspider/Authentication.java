package com.rapid7.appspider;

import org.json.JSONObject;

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
        if (response.getClass().equals(JSONObject.class)) {
            if (((JSONObject)response).getBoolean("IsSuccess")) {
                String authToken = ((JSONObject) response).getString("Token");
                return authToken;
            } else {
                return null;
            }
        }
        return null;
    }
}
