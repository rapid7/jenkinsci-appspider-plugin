/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URL;
import java.util.*;

public class ApiSerializer {

    private LoggerFacade logger;

    public ApiSerializer(LoggerFacade logger) {
        if (Objects.isNull(logger))
            throw new IllegalArgumentException("logger cannot be null");
        this.logger = logger;
    }

    /**
     * if jsonObject contains "IsSuccess" with value of true then value of "Token"
     * if non-empty is returned within an Optional; otherwise Optional.empty()
     * @param jsonObject JSON object containing "IsSuccess" and "Token"
     * @return Optional containing "Token" if "IsSuccess" has value of true
     *         and Token is not empty; otherwise Optional.empty()
     */
    public Optional<String> getTokenOrEmpty(JSONObject jsonObject) {
        if (!getIsSuccess(jsonObject))
            return Optional.empty();
        try {
            String token = jsonObject.getString("Token");
            return (Objects.isNull(token) || token.isEmpty())
                    ? Optional.empty()
                    : Optional.of(token);
        } catch (JSONException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }

    /**
     * returns the value of "IsSuccess" from the provided jsonObject if non-null; otherwise, false
     * @param jsonObject JSON object containing the "IsSuccess" value to extract
     * @return value of "IsSuccess" from the provided jsonObject if non-null; otherwise, false
     */
    public boolean getIsSuccess(JSONObject jsonObject) {
        if (Objects.isNull(jsonObject))
            return false;
        try {
            return jsonObject.getBoolean("IsSuccess");
        } catch (JSONException e) {
            logger.println(e.toString());
            return false;
        }
    }

    /**
     * returns the JSONObject for the item in configs with "Name" matching name if found;
     * otherwise Optional.empty()
     * @param configs JSONArray to search through
     * @param configName name of config to find
     * @return Optional containing the matching JSONObject on success; otherwise Optional.empty()
     */
    public Optional<JSONObject> findByConfigName(JSONArray configs, String configName) {
        if (Objects.isNull(configs))
            throw new IllegalArgumentException("configs cannot be empty");
        if (Objects.isNull(configName) || configName.isEmpty())
            throw new IllegalArgumentException("configName cannot be null or empty");

        for (Object object : configs) {
            if (!(object instanceof JSONObject))
                continue;
            JSONObject config = (JSONObject)object;
            if (config.getString("Name").equalsIgnoreCase(configName))
                return Optional.of(config);
        }
        logger.println("no config with name " + configName + " was found.");
        return Optional.empty();
    }

    /**
     * returns List{String} of all scan config names found in configs
     * @param configs JSONArray of scan config JSONObjects containing "Name" key
     * @return Optional containing List{String} which in turn contains all "Name" keys found
     *         in configs - if configs is non-null; Otherwise, Optional.empty()
     */
    public Optional<List<String>> getConfigNames(JSONArray configs) {
        if (Objects.isNull(configs))
            return Optional.empty();
        List<String> names = new ArrayList<>();
        for (Object object : configs) {
            if (!(object instanceof JSONObject))
                continue;
            JSONObject config = (JSONObject)object;
            try {
                String name = config.getString("Name");
                if (Objects.isNull(name) || name.isEmpty())
                    continue;
                names.add(name);
            } catch (JSONException e) {
                logger.println(e.toString());
                e.printStackTrace();
            }
        }
        return Optional.of(names);
    }

    /**
     * constructs scan config XML document using template with provided name and target
     * @param template template used to produce XML
     * @param name name of the new scan config
     * @param target target of the scan config
     * @return String representing scan config in XML format
     * @throws IOException thrown if I/O error occurs during template processing
     * @throws TemplateException if a problem occurs during template processing
     * @throws IllegalArgumentException if any of the provided arguments are null, or in the case of Strings empty
     */
    public String getScanConfigXml(Template template, String name, String target) throws IOException, TemplateException {
        if (Objects.isNull(template))
            throw new IllegalArgumentException("template cannot be null");
        if (Objects.isNull(name) || name.isEmpty())
            throw new IllegalArgumentException("name cannot be null or empty");
        if (Objects.isNull(target) || target.isEmpty())
            throw new IllegalArgumentException("targetURL cannot be null");

        URL targetURL = new URL(target);
        Map<String, String> templateData = new HashMap<>();
        templateData.put("name", name);
        templateData.put("url", targetURL.toString());

        URL urlWildcardPath;
        URL urlWildcardSubdomain;
        if(targetURL.getAuthority().endsWith("/")) {
            urlWildcardPath = new URL(targetURL.toString()+"*");
            urlWildcardSubdomain = new URL(targetURL.getProtocol() +
                    "://*." + targetURL.getAuthority() + "*");
        } else {
            urlWildcardPath = new URL(targetURL.toString()+"/*");
            urlWildcardSubdomain = new URL(targetURL.getProtocol() +
                    "://*." + targetURL.getAuthority() + "/*");
        }
        templateData.put("url_wildcard_path", urlWildcardPath.toString());
        templateData.put("url_wildcard_subdomain", urlWildcardSubdomain.toString());

        StringWriter scanConfigXml = new StringWriter();
        template.process(templateData, scanConfigXml);

        return scanConfigXml.toString();
    }

    public Optional<String> getScanConfigId(JSONObject config) {
        if (Objects.isNull(config))
            return Optional.empty();

        try {
            return Optional.of(config.getString("Id"));
        } catch (JSONException e) {
            logger.println(e.toString());
            return Optional.empty();
        }
    }
}
