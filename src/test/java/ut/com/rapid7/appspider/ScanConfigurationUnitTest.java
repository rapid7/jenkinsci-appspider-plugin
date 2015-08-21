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
        String name = "NicoTest2";
        String str_url = "http://www.webscantest.com";
        String engineGroupId = "51b6553a-ca34-4bf6-960a-f08d15597d07";
        Object config = ScanConfiguration.saveConfig(restUrl, authToken,
                name, str_url, engineGroupId);
        assertEquals(JSONObject.class, config.getClass());
    }
}
