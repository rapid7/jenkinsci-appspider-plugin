package ut.com.rapid7.appspider;

import org.json.JSONObject;
import org.junit.Test;
import com.rapid7.appspider.ScanManagement;

import static org.junit.Assert.assertEquals;

/**
 * Created by nbugash on 09/07/15.
 */
public class ScanManagementUnitTest extends BaseUnitTest {

    @Test
    public void getScans() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        Object response = ScanManagement.getScans(restUrl, authToken);
        assertEquals(JSONObject.class, response.getClass());
    }

    @Test
    public void runScanByConfigId() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        String configId = getConfigId();
        Object response = ScanManagement.runScanByConfigId(restUrl, authToken, configId);
        assertEquals(response.getClass(), JSONObject.class);
    }

    @Test
    public void runScanByConfigName() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        String configName = getConfigName();
        Object response = ScanManagement.runScanByConfigName(restUrl, authToken, configName);
        assertEquals(response.getClass(), JSONObject.class);
        assertEquals(((JSONObject) response).getBoolean("IsSuccess"), true);
    }

    @Test
    public void getScanStatus() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        String scanId = getScanId();
        Object response = ScanManagement.getScanStatus(restUrl, authToken, scanId);
        assertEquals(response.getClass(), JSONObject.class);
        assertEquals(((JSONObject) response).getBoolean("IsSuccess"), true);
        assertEquals(((JSONObject) response).getString("Status"), "Completed");
    }

    @Test
    public void hasReport() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        String scanId = getScanId();
        Object response = ScanManagement.hasReport(restUrl, authToken, scanId);
        assertEquals(response.getClass(), JSONObject.class);
        assertEquals(((JSONObject) response).getBoolean("IsSuccess"), true);
        assertEquals(((JSONObject) response).getBoolean("Result"), true);
    }
}
