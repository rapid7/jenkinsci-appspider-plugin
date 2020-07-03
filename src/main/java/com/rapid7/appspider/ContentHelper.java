/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import org.apache.commons.io.IOUtils;
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
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

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
            logger.println(e.toString());
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
     * @throws UnsupportedEncodingException thrown by StringEntity
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
        return isSuccessStatusCode(response)
            ? asJson(response.getEntity())
            : Optional.empty();
    }

    /**
     * converts the provided HttpEntity to a JSONObject
     * @param entity entity to convert
     * @return on success an Optional containing a JSONObject; otherwise, Optional.empty()
     */
    public Optional<JSONObject> asJson(HttpEntity entity) {
        if (entity.getContentType().getValue().contains(MediaType.APPLICATION_JSON)) {
            try {
                return Optional.of(new JSONObject(EntityUtils.toString(entity)));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return Optional.empty();
    }
    /**
     * extracts the key/value pairs from JSONObject and returns them as a Map{String, String}
     * @param optionalJsonObject Optional{JSONObject} to extract key/value pairs from if present
     * @return on successs a Map{String, String} of key/value pairs from JSONObject;
     *         otherwiwse Optional.empty()
     */
    public Optional<Map<String, String>> asIdToNameMapOfStringToString(Optional<JSONObject> optionalJsonObject) {
        return optionalJsonObject.flatMap(json ->
        {
            try {
                return Optional.of(json.getJSONArray("EngineGroups")
                    .toList()
                    .stream()
                    .filter(obj -> obj instanceof JSONObject)
                    .map(obj -> (JSONObject) obj)
                    .collect(Collectors.toMap(
                        obj -> obj.getString("Id"),
                        obj -> obj.getString("Name"))));
            } catch (JSONException e) {
                logger.println(e.toString());
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
        String contentType = entity.getContentType().getValue();
        if (!contentType.contains(MediaType.TEXT_HTML) && !contentType.contains(MediaType.TEXT_XML))
            return Optional.empty();

        try (StringWriter writer = new StringWriter()) {
            IOUtils.copy(new InputStreamReader(entity.getContent(), StandardCharsets.UTF_8), writer);
            return Optional.of(writer.toString());
        } catch (IOException e) {
            logger.println(e.toString());
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
            logger.println(e.toString());
            return Optional.empty();
        }
    }
}
