package com.rapid7.appspider;


import org.json.JSONArray;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by nbugash on 19/08/15.
 */
public class ScanEngineGroup extends Base {
    public final static String GETALLENGINEGROUPS = "/EngineGroup/GetAllEngineGroups";
    public final static String GETENGINEGROUPSFORCLIENT = "/EngineGroup/GetEngineGroupsForClient";

    public static Map<String,String> getAllEngineGroups(String restUrl, String authToken) {
        String apiCall = restUrl + GETALLENGINEGROUPS;
        Object response = get(apiCall, authToken);
        if (response.getClass().equals(JSONObject.class)) {
            Map<String,String> engines = new HashMap<String, String>();
            JSONArray engineGroups = ((JSONObject)response).getJSONArray("EngineGroups");
            for (int i = 0; i < engineGroups.length(); i++ ) {
                String engine_id = engineGroups.getJSONObject(i).getString("Id");
                String engine_name = engineGroups.getJSONObject(i).getString("Name");
                engines.put(engine_id,engine_name);
            }
            return engines;
        }
        return null;
    }
}
