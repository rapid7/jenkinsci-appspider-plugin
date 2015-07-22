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
        String authToken = getAuthToken();
        Object response = ScanManagement.getScans(this.getRestUrl(), authToken);
        assertEquals(JSONObject.class, response.getClass());
    }

    @Test
    public void runScanByConfigId() {
        String authToken = getAuthToken();
        Object response = ScanManagement.runScanByConfigId(this.getRestUrl(), authToken, this.getConfigId());
        assertEquals(response.getClass(), JSONObject.class);
    }

    @Test
    public void runScanByConfigName() {
        String authToken = getAuthToken();
        Object response = ScanManagement.runScanByConfigName(this.getRestUrl(), authToken, this.getConfigName());
        assertEquals(response.getClass(), JSONObject.class);
        assertEquals(((JSONObject) response).getBoolean("IsSuccess"), true);
    }

    @Test
    public void getScanStatus() {
        String authToken = getAuthToken();
        JSONObject scan = ScanManagement.runScanByConfigName(this.getRestUrl(), authToken, this.getConfigName());
        String scanId = scan.getJSONObject("Scan").getString("Id");
        Object response = ScanManagement.getScanStatus(this.getRestUrl(), authToken, scanId);
        assertEquals(response.getClass(), JSONObject.class);
        assertEquals(((JSONObject) response).getBoolean("IsSuccess"), true);
    }

    @Test
    public void hasReport() {
        String authToken = getAuthToken();
        JSONObject scan = ScanManagement.runScanByConfigName(this.getRestUrl(), authToken, this.getConfigName());
        String scanId = scan.getJSONObject("Scan").getString("Id");
        Object response = ScanManagement.hasReport(this.getRestUrl(), authToken, scanId);
        assertEquals(response.getClass(), JSONObject.class);
        assertEquals(((JSONObject) response).getBoolean("IsSuccess"), true);
    }
}
