package com.rapid7.appspider;

/**
 * Created by nbugash on 08/07/15.
 */
public class Authentication extends Base {

    private final static String API = "/Authentication/Login";

    /**
     * @param restUrl
     * @param username
     * @param password
     * @return the authorization token
     */
    public static String authenticate(String restUrl, String username, String password) {
        String apiCall = restUrl + API;
        Object response = post(apiCall, username, password);
        if (response.getClass().equals(String.class)) {
            String authToken = (String) response;
            return authToken;
        }
        return null;
    }
}
