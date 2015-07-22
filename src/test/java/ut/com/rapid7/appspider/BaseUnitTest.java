package ut.com.rapid7.appspider;

import com.rapid7.appspider.*;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Created by nbugash on 08/07/15.
 */
public class BaseUnitTest {
    private static String restUrl = "http://ontesting.ntobjectives.com/ntoe36/rest/v1";
    private static String username = "wstclient";
    private static String password = "wstclient";
    private static String configName = "wst3linksXSS_nb2";
    private static String configId = "dfd5dc78-7eec-46f2-a8ef-7580b778ea02";
    private static String scanId = "957285b2-89dd-42a7-bf6c-9f17c470703a";

    public static String getRestUrl() { return restUrl; }
    public static String getUsername() { return username; }
    public static String getPassword() { return password; }
    public static String getConfigName() {
        return configName;
    }
    public static String getConfigId() { return configId; }
    public static String getScanId() { return scanId; }

    public String getAuthToken() {
        return Authentication.authenticate(this.restUrl, this.username, this.password);
    }


}
