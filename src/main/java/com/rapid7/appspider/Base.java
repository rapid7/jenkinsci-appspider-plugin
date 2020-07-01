package com.rapid7.appspider;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.*;

import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Created by nbugash on 08/07/15.
 */
public class Base {

    private final static int SUCCESS = 200;
    private final static HttpClient CLIENT = new HttpClientFactory().getClient();

    /**
     * @param apiCall
     * @param authToken
     * @param params
     * @return JSON Object of the Restful api call
     */
    public static Object get(String apiCall, String authToken, Map<String, String> params) {
        // Receive the response from AppSpider
        HttpResponse getResponse = getResponse(apiCall, authToken, params);
        if (getResponse == null) {
            return null;
        }

        int statusCode;
        if (SUCCESS == (statusCode = getResponse.getStatusLine().getStatusCode())) {
            return getClassType(getResponse); // Return a JSONObject of the response
        } else {
            throw new RuntimeException("Failed! HTTP error code: " + statusCode);
        }
    }

    /**
     * @param apiCall
     * @param authToken
     * @param params
     * @return InputStream which can be used to read response from apiCall
     */
    public static InputStream getInputStreamReader(String apiCall, String authToken, Map<String, String> params) {
        HttpResponse getResponse = getResponse(apiCall, authToken, params);
        if (getResponse == null) {
            return null;
        }
        int statusCode;
        if (SUCCESS != (statusCode = getResponse.getStatusLine().getStatusCode())) {
            throw new RuntimeException("Failed! HTTP error code: " + statusCode);
        } 

        try {
            return getResponse.getEntity().getContent();
        }
        catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        catch (UnsupportedOperationException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * @param apiCall
     * @param authToken
     * @return JSONObject result of apiCall
     */
    public static JSONObject get(String apiCall, String authToken) {
        try {
            //Create HTTP Client
            // Initialized the get request
            HttpGet getRequest = new HttpGet(apiCall);
            getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            getRequest.addHeader("Authorization", "Basic " + authToken);

            // Receive the response from AppSpider
            HttpResponse getResponse = CLIENT.execute(getRequest);
            int statusCode = getResponse.getStatusLine().getStatusCode();
            if (statusCode == SUCCESS) {
                // Return a JSONObject of the response
                // return new JSONObject(EntityUtils.toString(getResponse.getEntity()));
                return (JSONObject) getClassType(getResponse);
            } else {
                throw new RuntimeException("Failed! HTTP error code: " + statusCode);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param apiCall Restful url of AppSpider Enterprise
     * @param username Username to log into AppSpider Enterprise
     * @param password Password
     * @return Authentication Token from AppSpider
     */
    public static JSONObject post(String apiCall, String username, String password) {
        try {

            //Initialize the post request
            HttpPost postRequest = new HttpPost(apiCall);
            postRequest.addHeader("Content-Type", "application/json");
            postRequest.setEntity(
                    new StringEntity(new JSONObject()
                            .put("name", username)
                            .put("password", password).toString()));

            // Receive the response for AppSpider
            HttpResponse postResponse = CLIENT.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (statusCode == SUCCESS) {
                // Obtain the JSON Object of the response
                return (JSONObject) getClassType(postResponse);
            } else {
                throw new RuntimeException("Failed! HTTP error code: " + statusCode);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }


    /**
     * @param apiCall
     * @param authToken
     * @param params
     * @return on success the class type of post response
     */
    public static Object post(String apiCall, String authToken, HashMap<String,String> params ) {
        try {
            /* Setup the request body */
            StringWriter scanConfigJsonRequest = new StringWriter();
            Template template = new Configuration().getTemplate(
                    "src/main/java/com/rapid7/appspider/template/scanConfigJsonRequestTemplate.ftl");
            template.process(params,scanConfigJsonRequest);

            String boundary = "-----" + new Date().getTime();
            HttpEntity entity = MultipartEntityBuilder.create()
                    .setBoundary(boundary)
                    .addTextBody("config", scanConfigJsonRequest.toString(), ContentType.APPLICATION_JSON)
                    .build();

            HttpPost postRequest = new HttpPost(apiCall);
            postRequest.addHeader("Authorization", "Basic " + authToken);
            postRequest.addHeader("Accept", "application/json");
            postRequest.setEntity(entity);

            HttpResponse postResponse = CLIENT.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (statusCode == SUCCESS) {
                return getClassType(postResponse);
            } else {
                throw new RuntimeException("Failed! HTTP error code: " + statusCode);
            }


        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param apiCall
     * @param authToken
     * @param params
     * @return on success the class type of post response
     */
    public static Object post(String apiCall, String authToken, Map<String, String> params) {
        try {

            HttpPost postRequest = new HttpPost(apiCall);

            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.addHeader("Authorization", "Basic " + authToken);

            if (params != null) {
                ArrayList<BasicNameValuePair> urlParameters = new ArrayList<BasicNameValuePair>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                postRequest.setEntity(new UrlEncodedFormEntity(urlParameters));
            }

            HttpResponse postResponse = CLIENT.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (statusCode == SUCCESS) {
                return getClassType(postResponse);
            } else {
                throw new RuntimeException("Failed! HTTP error code: " + statusCode);
            }

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
    /**
     * @param response
     * @return
     */
    private static Object getClassType(HttpResponse response) {
        String contentType = response.getEntity().getContentType().getValue();
        if (contentType.contains(MediaType.APPLICATION_JSON)) {
            try {
                return new JSONObject(EntityUtils.toString(response.getEntity()));
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (contentType.contains(MediaType.TEXT_HTML) || contentType.contains(MediaType.TEXT_XML)) {
            try {
                StringWriter writer = new StringWriter();
                IOUtils.copy(new InputStreamReader(response.getEntity().getContent(), StandardCharsets.UTF_8), writer);
                return writer.toString();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("Invalid content-type header");
        }
        return null;
    }

    /**
     * @param apiCall
     * @param authToken
     * @param params
     * @return HttpResponse result from get request to apiCall
     */
    private static HttpResponse getResponse(String apiCall, String authToken, Map<String, String> params) {
        try {

            // Create HttpGet request
            HttpGet getRequest;
            if (params != null) {
                URIBuilder uriBuilder = new URIBuilder(apiCall);
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    uriBuilder.addParameter(entry.getKey(), entry.getValue());
                }
                getRequest = new HttpGet(uriBuilder.build());
            } else {
                // Initialized the get request
                getRequest = new HttpGet(apiCall);
            }

            // Add the authentication token
            getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            getRequest.addHeader("Authorization", "Basic " + authToken);

            // Receive the response from AppSpider
            return CLIENT.execute(getRequest);

        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }


}
