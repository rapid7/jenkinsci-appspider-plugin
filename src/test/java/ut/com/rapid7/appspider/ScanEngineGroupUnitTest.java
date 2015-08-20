package ut.com.rapid7.appspider;

import org.jfree.util.HashNMap;
import org.junit.Test;
import com.rapid7.appspider.ScanEngineGroup;

import java.util.HashMap;

import static org.junit.Assert.assertEquals;

/**
 * Created by nbugash on 19/08/15.
 */
public class ScanEngineGroupUnitTest extends BaseUnitTest {

    @Test
    public void getAllEngineGroups(){
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        Object response = ScanEngineGroup.getAllEngineGroups(restUrl, authToken);
        assertEquals(HashMap.class, response.getClass());
    }

    @Test
    public void getEngineGroupsForClient() {
        String restUrl = getRestUrl();
        String authToken = getAuthToken();
        Object response = ScanEngineGroup.getEngineGroupsForClient(restUrl, authToken);
        assertEquals(HashMap.class, response.getClass());
    }
}
