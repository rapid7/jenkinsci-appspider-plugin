package ut.com.rapid7.appspider;

import com.rapid7.appspider.Authentication;
import com.rapid7.appspider.ScanManagement;
import com.rapid7.appspider.ReportManagement;
import org.json.JSONObject;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by nbugash on 09/07/15.
 */
public class ReportManagementUnitTest extends BaseUnitTest {

    @Test
    public void getVulnerabilitiesSummaryXml() {
        String authToken = Authentication.authenticate(this.getRestUrl(), this.getUsername(), this.getPassword());
        JSONObject jsonResponse = ScanManagement.runScanByConfigId(this.getRestUrl(),authToken,this.getConfigName());
        String scanId = jsonResponse.getJSONObject("Scan").getString("Id");
        Object response = ReportManagement.getVulnerabilitiesSummaryXml(this.getRestUrl(), authToken, scanId);
        assertEquals(response.getClass(), String.class);
    }
}
