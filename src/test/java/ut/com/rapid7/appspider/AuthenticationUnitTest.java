package ut.com.rapid7.appspider;

import com.rapid7.appspider.*;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

/**
 * Created by nbugash on 08/07/15.
 */
public class AuthenticationUnitTest extends BaseUnitTest {

    @Test
    public void authenticate() {
        Object authToken = Authentication.authenticate(this.getRestUrl(),
                this.getUsername(), this.getPassword());
        assertEquals(String.class, authToken.getClass());
    }
}
