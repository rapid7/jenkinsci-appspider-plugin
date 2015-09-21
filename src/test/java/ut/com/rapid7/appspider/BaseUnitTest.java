package ut.com.rapid7.appspider;

import com.rapid7.appspider.*;
import org.json.JSONObject;
import org.junit.Test;

/**
 * Created by nbugash on 08/07/15.
 */
public class BaseUnitTest {

    private static String restUrl = System.getenv("RESTURL");
    private static String username = System.getenv("USERNAME");
    private static String password = System.getenv("PASSWORD");
    private static String configName = System.getenv("CONFIGNAME");
    private static String configId = System.getenv("CONFIGID");
    private static String scanId = System.getenv("SCANID");

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
