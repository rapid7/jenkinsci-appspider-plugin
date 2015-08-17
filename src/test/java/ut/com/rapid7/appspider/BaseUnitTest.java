package ut.com.rapid7.appspider;

import com.rapid7.appspider.*;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Created by nbugash on 08/07/15.
 */
public class BaseUnitTest {
    private static String restUrl = "http://10.4.87.166/AppSpiderEnterprise/rest/v1";
    private static String username = "nbugash";
    private static String password = "nbugash1";
    private static String configName = "webscantest";
    private static String configId = "dc12468d-5e5f-4eb1-884c-bcc324a7b0e6";
    private static String scanId = "5179b001-d995-4ce2-a2ef-1917e8ee179f";

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
