/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
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
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class HttpClientService implements ClientService {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCEPT_HEADER = "Accept";
    private static final String CONTENT_TYPE_HEADER = "Context-Type";
    private static final String APPLICATION_JSON = "application/json";

    private final HttpClient httpClient;
    private final LoggerFacade logger;
    private final ContentHelper contentHelper;

    public HttpClientService(HttpClient httpClient, ContentHelper contentHelper, LoggerFacade logger) {
        if (Objects.isNull(httpClient))
            throw new IllegalArgumentException("httpClient cannot be null");
        if (Objects.isNull(contentHelper))
            throw new IllegalArgumentException("jsonHelper cannot be null");
        if (Objects.isNull(logger))
            throw new IllegalArgumentException("logger cannot be null");

        this.httpClient = httpClient;
        this.contentHelper = contentHelper;
        this.logger = logger;
    }

    /**
     * executes the provided HttpRequestBase returning the result as a JSONObject
     * @param request the request to send/execute
     * @return on success an Optional containing a JSONObject; otherwise, Optional.empty()
     */
    @Override
    public Optional<JSONObject> executeJsonRequest(HttpRequestBase request) {
        try {
            return contentHelper.responseToJSONObject(httpClient.execute(request));

        } catch (IOException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    /**
     * executes the provided HttpRequestBase returning the result as a HttpEntity
     * @param request the request to send/execute
     * @return on success an Optional containing a HttpEntity; otherwise, Optional.empty()
     */
    public Optional<HttpEntity> executeEntityRequest(HttpRequestBase request) {
        try {
            HttpResponse response = httpClient.execute(request);
            int statusCode = response.getStatusLine().getStatusCode();
            if (statusCode != HttpStatus.SC_OK)
                throw new RuntimeException("Failed! HTTP error code: " + statusCode);

            return Optional.of(response.getEntity());

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
        request.addHeader(CONTENT_TYPE_HEADER, "application/x-www-form-urlencoded");
        request.addHeader(AUTHORIZATION_HEADER, "Basic " + authToken);

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
            builder.addParameters(Arrays.asList(params));

            return buildGetRequestUsingFormUrlEncoding(builder.build().toString(), authToken);

        } catch (URISyntaxException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    /**
     * builds a HttpGet request object for the given endpoint using authToken as basic authentication header.
     * Request is sent using Content-Type and Accept as Application/json
     * @param endpoint endpoint to perform get request on
     * @param authToken authorization token for basic authentication
     * @return Optional of HttpGet containing the request object
     * @throws IllegalArgumentException when either endpoint or authToken are null or empty
     */
    @Override
    public Optional<HttpGet> buildGetAcceptionApplicatonJson(String endpoint, String authToken) {
        ensureArgumentsValid(endpoint, authToken);

        HttpGet request = new HttpGet(endpoint);
        request.addHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON); // not strictly required but also no harm
        request.addHeader(ACCEPT_HEADER, APPLICATION_JSON);
        request.addHeader(AUTHORIZATION_HEADER, "Basic " + authToken);
        return Optional.of(request);
    }

    /**
     * builds a HttpPost request object for the given endpoint containing the provided body content
     * body will be posted using Content-Type of application/json
     * @param endpoint endpoint to perform post request on
     * @param body JSON body to be sent with the request
     * @return Optional of HttpPost containg the request object
     */
    @Override
    public Optional<HttpPost> buildPostRequestUsingApplicationJson(String endpoint, HttpEntity body) {
        ensureArgumentsValid(endpoint, body);

        HttpPost request = new HttpPost(endpoint);
        request.addHeader(CONTENT_TYPE_HEADER, APPLICATION_JSON);
        request.setEntity(body);
        return Optional.of(request);
    }

    /**
     * builds a HttpPost request object for the given endpoint containing the provided body content
     * body will be posted using Content-Type as application/x-www-form-urlencoded
     * @param endpoint endpoint to perform post request on
     * @param authToken authorization token for basic authentication
     * @param params name/value pairs sent as the entity of the request
     * @return Optional of HttpPost containg the request object
     */
    @Override
    public Optional<HttpPost> buildPostRequestUsingFormUrlEncoding(String endpoint, String authToken, NameValuePair... params) {
        ensureArgumentsValid(endpoint, authToken);

        HttpPost request = new HttpPost(endpoint);
        request.addHeader(CONTENT_TYPE_HEADER, "application/x-www-form-urlencoded");
        request.addHeader(AUTHORIZATION_HEADER, "Basic " + authToken);

        try {
            request.setEntity(new UrlEncodedFormEntity(Arrays.asList(params)));
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
