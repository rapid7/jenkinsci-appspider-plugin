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
        String authToken = this.getAuthToken();
        Object configs = ScanConfiguration.getConfigs(this.getRestUrl(), authToken);

        assertEquals(JSONObject.class, configs.getClass());
    }
}
