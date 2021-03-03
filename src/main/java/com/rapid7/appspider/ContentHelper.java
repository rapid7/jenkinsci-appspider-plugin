/*
 * Copyright © 2003 - 2021 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import org.apache.commons.io.IOUtils;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import javax.ws.rs.core.MediaType;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.rapid7.appspider.Utility.isSuccessStatusCode;

/**
 * parsing and serializing helper methods for handling JSONObject manipulation
 */
public class ContentHelper {

    private final LoggerFacade logger;

    public ContentHelper(LoggerFacade logger) {
        if (Objects.isNull(logger))
            throw new IllegalArgumentException("logger cannot be null");
        this.logger = logger;
    }


    /**
     * returns JSONArray from jsonObject matching key
     * @param jsonObject JSONObject containing JSONArray using key
     * @param key key of the JSONArray within jsonObject
     * @return Optional containing JSONArray if present; otherwise Optional.empty()
     */
    public Optional<JSONArray> getArrayFrom(JSONObject jsonObject, String key) {
        if (Objects.isNull(jsonObject) || Objects.isNull(key) || key.isEmpty())
            return Optional.empty();
        try {
            JSONArray array = jsonObject.getJSONArray(key);
            return Optional.of(array);
        } catch (JSONException e) {
            logger.severe(e.toString());
            return Optional.empty();
        }
    }

    /**
     * builds a simple JSON object from name value pairs
     * @param pairs name value pairs to add to the new JSON Object
     * @return JSONObject constructed from pairs
     */
    public JSONObject jsonFrom(NameValuePair... pairs) {
        JSONObject jsonObject = new JSONObject();
        for (NameValuePair pair : pairs) {
            jsonObject.put(pair.getName(), pair.getValue());
        }
        return jsonObject;
    }

    /**
     * builds a StringEntity using the string provided by jsonObject.toString()
     * @param jsonObject jsonObject which provides String via toString
     * @return StringEntity containing the string value of jsonObject
     */
    public StringEntity entityFrom(JSONObject jsonObject) {
        if (Objects.isNull(jsonObject))
            throw new IllegalArgumentException("jsonObject cannot be null");
        return new StringEntity(jsonObject.toString(), ContentType.APPLICATION_JSON);
    }

    /**
     * simple wrapper around entityFrom(JSONObject) that uses jsonFrom to build the JSONObject
     * @param pairs passed to jsonFrom
     * @return entity returned from entityFrom(JSONObject)
     * @throws UnsupportedEncodingException thrown from entityFrom(JSONObject)
     */
    public StringEntity entityFrom(NameValuePair... pairs) throws UnsupportedEncodingException {
        return entityFrom(jsonFrom(pairs));
    }

    /**
     * simple wrapper around BasicNameValuePair constructor provided to shorten syntax
     * @param key key of the new NameValuePair
     * @param value value of the new NameValuePair
     * @return NameValuePair of key and value
     */
    public NameValuePair pairFrom(String key, String value) {
        return new BasicNameValuePair(key, value);
    }

    /**
     * extracts the response content into a JSONObject
     * @param response response to extract JSONObject from
     * @return on success an Optional containing a JSONObject; otherwise, Optional.empty()
     */
    public Optional<JSONObject> responseToJSONObject(HttpResponse response) {
        if (isSuccessStatusCode(response)) {
            return asJson(response.getEntity());
        }

        logResponseFailure("request failed", response);
        return Optional.empty();
    }

    /**
     * converts the provided HttpEntity to a JSONObject
     * @param entity entity to convert
     * @return on success an Optional containing a JSONObject; otherwise, Optional.empty()
     */
    public Optional<JSONObject> asJson(HttpEntity entity) {
        if (getContentTypeOrEmpty(entity).orElse("").contains(MediaType.APPLICATION_JSON)) {
            try {
                return Optional.of(new JSONObject(EntityUtils.toString(entity)));
            } catch (IOException e) {
                logger.severe(e.toString());
            }
        }
        return Optional.empty();
    }

    /**
     * extracts the key/value pairs from JSONObject and returns them as a Map{String, String}
     * @param key key in the json object to serve as key in the map
     * @param value value in the json object to serve as value in the map
     * @param optionalJsonObject Optional{JSONObject} to extract key/value pairs from if present
     * @return on successs a Map{String, String} of key/value pairs from JSONObject;
     *         otherwiwse Optional.empty()
     */
    public Optional<Map<String, String>> asMapOfStringToString(String key, String value, Optional<JSONObject> optionalJsonObject) {
        return optionalJsonObject.flatMap(json ->
        {
            try {
                JSONArray array = json.getJSONArray("EngineGroups");
                Map<String, String> mapOfStringToString = new HashMap<>();
                if (Objects.isNull(array))
                    return Optional.empty();
                for (int i =0; i< array.length(); i++) {
                    JSONObject jsonObject = array.getJSONObject(i);
                    mapOfStringToString.put(jsonObject.getString(key), jsonObject.getString(value));
                }
                return Optional.of(mapOfStringToString);
            } catch (JSONException e) {
                logger.severe(e.toString());
                return Optional.empty();
            }
        });
    }

    /**
     * returns text/html or text/html content from enttity if found; otherwise Optional.empty()
     * @param entity entity containing the String content to return
     * @return Optional containing the String content of entity on success; otherwise, Optional.empty()
     */
    public Optional<String> getTextHtmlOrXmlContent(HttpEntity entity) {
        if (Objects.isNull(entity))
            return Optional.empty();
        String contentType = getContentTypeOrEmpty(entity).orElse("");
        if (!contentType.contains(MediaType.TEXT_HTML) && !contentType.contains(MediaType.TEXT_XML))
            return Optional.empty();

        try (StringWriter writer = new StringWriter()) {
            IOUtils.copy(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8), writer);
            return Optional.of(writer.toString());
        } catch (IOException e) {
            logger.severe(e.toString());
            return Optional.empty();
        }
    }

    /**
     * returns InputStream for the content of entity
     * @param entity entity to return InputStream of content for
     * @return Optional containing InputStream on success; otherwise, Optional.empty()
     */
    public Optional<InputStream> getInputStream(HttpEntity entity) {
        if (Objects.isNull(entity))
            return Optional.empty();
        try  {
            return Optional.of(entity.getContent());
        } catch (IOException e) {
            logger.severe(e.toString());
            return Optional.empty();
        }
    }

    private Optional<String> getContentTypeOrEmpty(HttpEntity entity) {
        if (Objects.isNull(entity)) {
            return Optional.empty();
        }
        Header contentType = entity.getContentType();
        if (Objects.isNull(contentType)) {
            return Optional.empty();
        }
        String value = contentType.getValue();
        return !Objects.isNull(value)
                ? Optional.of(value)
                : Optional.empty();
    }
    private void logResponseFailure(String introduction, HttpResponse response) {
        if (response == null) {
            logger.severe("Response is null");
            return;
        }

        JSONObject error = asJson(response.getEntity()).orElse(new JSONObject());
        try {
            final String MESSAGE_KEY = "Message";

            String errorMessage;
            if (error.has("ErrorMessage")) {
                errorMessage = error.getString("ErrorMessage");
            } else if (error.has(MESSAGE_KEY)) {
                errorMessage = error.getString(MESSAGE_KEY);
            } else {
                errorMessage = response.getStatusLine().toString();
            }
            String reason;
            if (error.has("Reason")) {
                reason = error.getString("Reason");
            } else if (error.has(MESSAGE_KEY)) {
                reason = error.getString(MESSAGE_KEY);
            } else {
                reason = response.getStatusLine().toString();
            }
            logger.severe(String.format("%s: '%s'.  with reason '%s'", introduction, errorMessage, reason));
        } catch (JSONException e) {
            logger.severe(String.format("%s: %s.%nexception: %s",
                    introduction, response.getStatusLine().getStatusCode(), e.toString()));
        }
    }
}
