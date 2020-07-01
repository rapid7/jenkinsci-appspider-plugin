/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import com.sun.tools.javac.util.List;
import org.apache.http.HttpEntity;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.client.utils.URIBuilder;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URISyntaxException;
import java.util.Objects;
import java.util.Optional;

class HttpClientService implements ClientService {

    private HttpClient httpClient;
    private LoggerFacade logger;
    private JsonHelper jsonHelper;

    HttpClientService(HttpClient httpClient, JsonHelper jsonHelper, LoggerFacade logger) {
        if (Objects.isNull(httpClient))
            throw new IllegalArgumentException("httpClient cannot be null");
        if (Objects.isNull(jsonHelper))
            throw new IllegalArgumentException("jsonHelper cannot be null");
        if (Objects.isNull(logger))
            throw new IllegalArgumentException("logger cannot be null");

        this.httpClient = httpClient;
        this.jsonHelper = jsonHelper;
        this.logger = logger;
    }

    /**
     * executes the provided HttpRequestBase returning the result as a JSONObject
     * @param request the request to send/execute
     * @return on success an Optional containing a JSONObject; otherwise, Optional.empty()
     */
    @Override
    public Optional<JSONObject> executeRequest(HttpRequestBase request) {
        try {
            return jsonHelper.responseToJSONObject(httpClient.execute(request));

        } catch (IOException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    /**
     * Builds a HttpGet request object for the endpoint given by endpoint using authToken as basic authentication header
     * @param endpoint endpoint to perform get request on
     * @param authToken authorization token for basic authentication
     * @return Optional of HttpGet containing the request object
     * @throws IllegalArgumentException when either endpoint or authToken are null or empty
     */
    @Override
    public Optional<HttpGet> buildGetRequestUsingFormUrlEncoding(String endpoint, String authToken) {
        ensureArgumentsValid(endpoint, authToken);

        HttpGet request = new HttpGet(endpoint);
        request.addHeader("Context-Type", "application/x-www-form-urlencoded");
        request.addHeader("Authorization", "Basic " + authToken);

        return Optional.of(request);
    }

    /**
     * builds a HttpGet request object for the given endpoint using authToken as basic authentication header and params
     * as additional URL key/value parameters
     * @param endpoint endpoint to perform get request on
     * @param authToken authorization token for basic authentication
     * @param params name/value pairs encoded and added to endpoint
     * @return Optional of HttpGet containing the request object
     * @throws IllegalArgumentException when either endpoint or authToken are null or empty
     */
    @Override
    public Optional<HttpGet> buildGetRequestUsingFormUrlEncoding(String endpoint, String authToken, NameValuePair... params) {
        if (Objects.isNull(params) || params.length == 0)
            return buildGetRequestUsingFormUrlEncoding(endpoint, authToken);

        ensureArgumentsValid(endpoint, authToken);
        try {
            URIBuilder builder = new URIBuilder(endpoint);
            builder.addParameters(List.from(params));

            return buildGetRequestUsingFormUrlEncoding(builder.build().toString(), authToken);

        } catch (URISyntaxException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    @Override
    public Optional<HttpGet> buildGetAcceptionApplicatonJson(String endpoint, String authToken) {
        ensureArgumentsValid(endpoint, authToken);

        HttpGet request = new HttpGet(endpoint);
        request.addHeader("Context-Type", "application/json"); // not strictly required but also no harm
        request.addHeader("Accept", "application/json");
        request.addHeader("Authorization", "Basic " + authToken);
        return Optional.of(request);
    }

    @Override
    public Optional<HttpPost> buildPostRequestUsingApplicationJson(String endpoint, HttpEntity body) {
        ensureArgumentsValid(endpoint, body);

        HttpPost request = new HttpPost(endpoint);
        request.addHeader("Content-Type", "application/json");
        request.setEntity(body);
        return Optional.of(request);
    }

    @Override
    public Optional<HttpPost> buildPostRequestUsingFormUrlEncoding(String endpoint, String authToken, NameValuePair... params) {
        ensureArgumentsValid(endpoint, authToken);

        HttpPost request = new HttpPost(endpoint);
        request.addHeader("Content-Type", "application/x-www-form-urlencoded");
        request.addHeader("Authorization", "Basic " + authToken);

        try {
            request.setEntity(new UrlEncodedFormEntity(List.from(params)));
            return Optional.of(request);

        } catch (UnsupportedEncodingException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    private static void ensureArgumentsValid(String endpoint, String authToken) {
        if (Objects.isNull(endpoint) || endpoint.isEmpty())
            throw new IllegalArgumentException("endpoint cannot be null or empty");
        if (Objects.isNull(authToken) || authToken.isEmpty())
            throw new IllegalArgumentException("authToken cannot be null or empty");
    }
    private static void ensureArgumentsValid(String endpoint, HttpEntity body) {
        if (Objects.isNull(endpoint) || endpoint.isEmpty())
            throw new IllegalArgumentException("endpoint cannot be null or empty");
        if (Objects.isNull(body))
            throw new IllegalArgumentException("body cannot be null");
    }

}
