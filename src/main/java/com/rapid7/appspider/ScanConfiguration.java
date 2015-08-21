package com.rapid7.appspider;

import org.json.JSONArray;
import org.json.JSONObject;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * Created by nbugash on 08/07/15.
 */
public class ScanConfiguration extends Base {

    private static final String SAVECONFIG = "/Config/SaveConfig";
    private static final String DELETECONFIG = "/Config/DeleteConfig";
    private static final String GETCONFIGS = "/Config/GetConfigs";
    private static final String GETSCANCONFIG = "/Config/GetScanConfig";
    private static final String GETATTACHMENT = "/Config/GetAttachment";
    private static final String GETATTACHMENTS = "/Config/GetAttachments";

    /**
     * @param restUrl
     * @param authToken
     * @return
     */
    public static JSONObject getConfigs(String restUrl, String authToken) {
        String apiCall = restUrl + GETCONFIGS;
        Object response = get(apiCall, authToken);
        if (response.getClass().equals(JSONObject.class)) {
            return (JSONObject) response;
        }
        return null;
    }

    /**
     * @param restUrl
     * @param authToken
     * @return
     */
    public static String[] getConfigNames(String restUrl, String authToken){
        JSONObject response = getConfigs(restUrl, authToken);
        JSONArray jsonConfigs = response.getJSONArray("Configs");
        String[] configNames = new String[jsonConfigs.length()];
        for (int i = 0; i < jsonConfigs.length(); i++) {
            configNames[i] = jsonConfigs.getJSONObject(i).getString("Name");
        }
        return configNames;
    }

    /**
     * @param restUrl
     * @param authToken
     * @param name
     * @param str_url
     * @param engineGroupId
     * @return
     * @throws MalformedURLException
     */
    public static JSONObject saveConfig(String restUrl, String authToken,
                                        String name, String str_url,
                                        String engineGroupId) {
        try {
            Map<String, String> data = new HashMap<String, String>();
            data.put("name", name);

            URL url = new URL(str_url);
            data.put("url",url.toString());

            URL url_wildcard_path;
            URL url_wildcard_subdomain;
            if(url.getAuthority().endsWith("/")) {
                url_wildcard_path = new URL(url.toString()+"*");
                url_wildcard_subdomain = new URL(url.getProtocol() +
                        "://*." + url.getAuthority() + "*");
            } else {
                url_wildcard_path = new URL(url.toString()+"/*");
                url_wildcard_subdomain = new URL(url.getProtocol() +
                        "://*." + url.getAuthority() + "/*");
            }
            data.put("url_wildcard_path", url_wildcard_path.toString());
            data.put("url_wildcard_subdomain", url_wildcard_subdomain.toString());

            StringWriter scanConfigXML = new StringWriter();
            Template template = new Configuration().getTemplate(
                    "src/main/java/com/rapid7/appspider/template/scanConfigTemplate.ftl");
            template.process(data, scanConfigXML);

            /* Continue with the api call*/
            String apiCall = restUrl + SAVECONFIG;


            /* Setup the HashMap request body */

            HashMap<String,String> params = new HashMap<String, String>();
            params.put("defendEnabled","true");
            params.put("monitoringDelay","0");
            params.put("monitoringTriggerScan","true");
            params.put("id","null");
            params.put("name",name);
            params.put("clientId","null");
            params.put("engineGroupId",engineGroupId);
            params.put("monitoring", "true");
            params.put("isApproveRequired", "false");
            params.put("scanconfigxml", scanConfigXML.toString());

            Object response = post(apiCall, authToken, params);
            if (response.getClass().equals(JSONObject.class)) {
                return (JSONObject) response;
            }
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (TemplateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }


        return null;
    }
}
