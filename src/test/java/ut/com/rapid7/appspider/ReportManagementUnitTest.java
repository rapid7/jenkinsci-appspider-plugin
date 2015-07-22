package ut.com.rapid7.appspider;
import com.rapid7.appspider.ReportManagement;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by nbugash on 09/07/15.
 */
public class ReportManagementUnitTest extends BaseUnitTest {

    @Test
    public void getVulnerabilitiesSummaryXml() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        String scanId = getScanId();
        Object response = ReportManagement.getVulnerabilitiesSummaryXml(restUrl, authToken, scanId);
        assertEquals(response.getClass(), String.class);
    }
}
