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
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.utils.URIBuilder;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.*;

import javax.ws.rs.core.MediaType;
import java.io.*;
import java.net.*;
import java.util.*;

/**
 * Created by nbugash on 08/07/15.
 */
public class Base {

    private final static int SUCCESS = 200;

    /**
     * @param apiCall
     * @param authToken
     * @param params
     * @return JSON Object of the Restful api call
     */
    public static Object get(String apiCall, String authToken, Map<String, String> params) {
        try {
            //Create HTTP Client
            HttpClient httpClient = HttpClientBuilder.create().build();

            // Create HttpGet request
            HttpGet getRequest;
            if (!params.equals(null)) {
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
            HttpResponse getResponse = httpClient.execute(getRequest);
            int statusCode = getResponse.getStatusLine().getStatusCode();
            if (statusCode == SUCCESS) {
                // Return a JSONObject of the response
                return getClassType(getResponse);
//                return new JSONObject(getResponse);
            } else {
                throw new RuntimeException("Failed! HTTP error code: " + statusCode);
            }
        } catch (ClientProtocolException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @param apiCall
     * @param authToken
     * @return
     */
    public static JSONObject get(String apiCall, String authToken) {
        try {
            //Create HTTP Client
            HttpClient httpClient = HttpClientBuilder.create().build();
            // Initialized the get request
            HttpGet getRequest = new HttpGet(apiCall);
            getRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            getRequest.addHeader("Authorization", "Basic " + authToken);

            // Receive the response from AppSpider
            HttpResponse getResponse = httpClient.execute(getRequest);
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
     * @param apiCall
     * @param username
     * @param password
     * @return Authentication Token from AppSpider
     */
    public static JSONObject post(String apiCall, String username, String password) {
        try {
            //Create HTTP Client
            HttpClient httpClient = HttpClientBuilder.create().build();

            //Initialize the post request
            HttpPost postRequest = new HttpPost(apiCall);
            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");

            ArrayList<BasicNameValuePair> params = new ArrayList<BasicNameValuePair>();
            params.add(new BasicNameValuePair("name", username));
            params.add(new BasicNameValuePair("password", password));
            postRequest.setEntity(new UrlEncodedFormEntity(params));
            // Receive the response for AppSpider
            HttpResponse postResponse = httpClient.execute(postRequest);
            int statusCode = postResponse.getStatusLine().getStatusCode();
            if (statusCode == SUCCESS) {
                // Obtain the JSON Object of the response
                JSONObject jsonResponse = (JSONObject) getClassType(postResponse);
                return jsonResponse;
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
     * @return
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

            HttpClient httpClient = HttpClientBuilder.create().build();
            HttpPost postRequest = new HttpPost(apiCall);
            postRequest.addHeader("Authorization", "Basic " + authToken);
            postRequest.addHeader("Accept", "application/json");
            postRequest.setEntity(entity);

            HttpResponse postResponse = httpClient.execute(postRequest);
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
     * @return
     */
    public static Object post(String apiCall, String authToken, Map<String, String> params) {
        try {
            HttpClient httpClient = HttpClientBuilder.create().build();

            HttpPost postRequest = new HttpPost(apiCall);

            postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
            postRequest.addHeader("Authorization", "Basic " + authToken);

            if (!params.equals(null)) {
                ArrayList<BasicNameValuePair> urlParameters = new ArrayList<BasicNameValuePair>();
                for (Map.Entry<String, String> entry : params.entrySet()) {
                    urlParameters.add(new BasicNameValuePair(entry.getKey(), entry.getValue()));
                }
                postRequest.setEntity(new UrlEncodedFormEntity(urlParameters));
            }

            HttpResponse postResponse = httpClient.execute(postRequest);
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
            JSONObject jsonResponse = null;
            try {
                jsonResponse = new JSONObject(EntityUtils.toString(response.getEntity()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            return jsonResponse;
        } else if (contentType.contains(MediaType.TEXT_HTML) || contentType.contains(MediaType.TEXT_XML)) {
            try {
                StringWriter writer = new StringWriter();
                IOUtils.copy(new InputStreamReader(response.getEntity().getContent()), writer);
                String xmlResponse = writer.toString();
                return xmlResponse;
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            throw new RuntimeException("Invalid content-type header");
        }
        return null;
    }

}
