/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ScanResult;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.StringWriter;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.*;

public class ApiSerializer {

    private final LoggerFacade logger;

    public ApiSerializer(LoggerFacade logger) {
        if (logger == null)
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
            logger.severe(e.toString());
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
            logger.severe(e.toString());
            return false;
        }
    }

    public ScanResult getScanResult(JSONObject jsonObject) {
        try {
            return new ScanResult(jsonObject);
        } catch (IllegalArgumentException e) {
            logger.severe(e.toString());
            return new ScanResult(false, "");
        }
    }

    /**
     * returns the value of "IsSuccess" and "Result" from the provided jsonObject if non-null; otherwise, false
     * @param jsonObject JSON object containing the "IsSuccess" value to extract
     * @return value of "IsSuccess" from the provided jsonObject if non-null; otherwise, false
     */
    public boolean getResultIsSuccess(JSONObject jsonObject) {
        return getBooleansFrom(jsonObject, "Result", "IsSuccess").orElse(false);
    }

    /**
     * returns the value of "Status" from the provided jsonObject if non-null and Status is not empty;
     * otherwise
     * @param jsonObject JSON object containing "Status" key
     * @return Optional containing value of "Status" from jsonObject if not empty on success; otherwise,
     *         Optional.empty()
     */
    public Optional<String> getStatus(JSONObject jsonObject) {
        try {
            if (Objects.isNull(jsonObject))
                return Optional.empty();
            String status = jsonObject.getString("Status");
            return Objects.isNull(status) || status.isEmpty()
                ? Optional.empty()
                : Optional.of(status);
        } catch (JSONException e) {
            logger.severe(e.toString());
            return Optional.empty();
        }
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
            if ((object instanceof JSONObject)) {
                JSONObject config = (JSONObject) object;
                try {
                    String name = config.getString("Name");
                    if (Objects.isNull(name) || name.isEmpty())
                        continue;
                    names.add(name);
                } catch (JSONException e) {
                    logger.severe(e.toString());
                }
            }
        }
        return Optional.of(names);
    }

    /**
     * constructs scan config XML document using template with provided name and target
     * @param template template used to produce XML
     * @param name name of the new scan config
     * @param targetURL target of the scan config
     * @return String representing scan config in XML format
     * @throws IOException thrown if I/O error occurs during template processing
     * @throws TemplateException if a problem occurs during template processing
     * @throws IllegalArgumentException if any of the provided arguments are null, or in the case of Strings empty
     * @throws MalformedURLException if target is not a va
     */
    public String getScanConfigXml(Template template, String name, URL targetURL) throws IOException, TemplateException {
        if (Objects.isNull(template))
            throw new IllegalArgumentException("template cannot be null");
        if (Objects.isNull(name) || name.isEmpty())
            throw new IllegalArgumentException("name cannot be null or empty");
        if (Objects.isNull(targetURL))
            throw new IllegalArgumentException("targetURL cannot be null");

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
            logger.severe(e.toString());
            return Optional.empty();
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
        try {
            if (Objects.isNull(configName) || configName.isEmpty())
                throw new IllegalArgumentException("configName cannot be null or empty");

            for (Object object : configs) {
                if (!(object instanceof JSONObject))
                    continue;
                JSONObject config = (JSONObject) object;
                if (config.getString("Name").equalsIgnoreCase(configName))
                    return Optional.of(config);
            }
            logger.println("no config with name " + configName + " was found.");
            return Optional.empty();
        } catch (JSONException e) {
            logger.severe(e.toString());
            return Optional.empty();
        }

    }

    /**
     * returns true if all given keys in jsonObject are true
     * @param jsonObject json object containing keys
     * @param keys keys for the boolean fields of jsonObject
     * @return true if all keys are present and true
     */
    public Optional<Boolean> getBooleansFrom(JSONObject jsonObject, String... keys) {
        return Objects.isNull(jsonObject)
            ? Optional.empty()
            : Optional
                .of(Arrays.stream(keys)
                .allMatch(jsonObject::getBoolean));
    }

}
