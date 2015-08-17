package ut.com.rapid7.appspider;

import org.json.JSONObject;
import com.rapid7.appspider.ScanConfiguration;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by nbugash on 08/07/15.
 */
public class ScanConfigurationUnitTest extends BaseUnitTest {

    @Test
    public void getConfigs() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        Object configs = ScanConfiguration.getConfigs(restUrl, authToken);
        assertEquals(JSONObject.class, configs.getClass());
    }
    @Test
    public void saveConfig() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        String name = "new_scan_config";
        String str_url = "http://examplesite.com";
        String engineGroupId = "9938563e-469c-4c75-963d-37ebe4113f62";
        Object config = ScanConfiguration.saveConfig(restUrl, authToken,
                name, str_url, engineGroupId);
        assertEquals(JSONObject.class, config.getClass());
    }
}
