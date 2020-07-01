/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.json.JSONObject;

import java.util.Map;
import java.util.Optional;

public interface ClientService {

    /**
     * executes the provided HttpRequestBase returning the result as a JSONObject
     * @param request the request to send/execute
     * @return on success an Optional containing a JSONObject; otherwise, Optional.empty()
     */
    Optional<JSONObject> executeRequest(HttpRequestBase request);

    /**
     * Builds a HttpGet request object for the endpoint given by endpoint using authToken as basic authentication header
     * @param endpoint endpoint to perform get request on
     * @param authToken authorization token for basic authentication
     * @return Optional of HttpGet containing the request object
     * @throws IllegalArgumentException when either endpoint or authToken are null or empty
     */
    Optional<HttpGet> buildGetRequestUsingFormUrlEncoding(String endpoint, String authToken);

    /**
     * builds a HttpGet request object for the given endpoint using authToken as basic authentication header and params
     * as additional URL key/value parameters
     * @param endpoint endpoint to perform get request on
     * @param authToken authorization token for basic authentication
     * @param params name/value pairs encoded and added to endpoint
     * @return Optional of HttpGet containing the request object
     * @throws IllegalArgumentException when either endpoint or authToken are null or empty
     */
    Optional<HttpGet> buildGetRequestUsingFormUrlEncoding(String endpoint, String authToken, NameValuePair... params);

    Optional<HttpGet> buildGetAcceptionApplicatonJson(String endpoint, String authToken);

    Optional<HttpPost> buildPostRequestUsingApplicationJson(String endpoint, HttpEntity body);

    Optional<HttpPost> buildPostRequestUsingFormUrlEncoding(String endpoint, String authToken, NameValuePair... params);
}
