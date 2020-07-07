package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ScanResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class StandardEnterpriseClientTest {

    private static final String url = "https://appspider.rapid7.com/AppSpiderEnterprise/rest/v1";
    private EnterpriseClientTestContext context;

    @BeforeEach
    public void initialize() {
        context = new EnterpriseClientTestContext(url);
    }

    @AfterEach
    public void cleanup() {
        context.close();
    }

    @Test
    void testAuthenticationReturnsTrueWhenCredentialsAreValid() throws IOException {
        context
            .arrangeExpectedValues(true)
            .configureLogin(true)
            .configureEnterpriseClient();

        assertTrue(context.getEnterpriseClient().testAuthentication("wolf359", "pa55word"));
    }
    @Test
    void testAuthenticationReturnsFalseWhenCredentialsAreInvalid() throws IOException {
        context
            .arrangeExpectedValues(false)
            .configureLogin(false)
            .configureEnterpriseClient();

        // act and assert
        assertFalse(context.getEnterpriseClient().testAuthentication("wolf359", "pa55word"));
    }
    @Test
    void loginHasTokenWhenCredentialsAreValid() throws IOException {
        context
            .arrangeExpectedValues(true)
            .configureLogin(true)
            .configureEnterpriseClient();
        assertTrue(context.getEnterpriseClient().login("wolf359", "pa55word").isPresent());
    }
    @Test
    void loginTokenHasExpectedValueWhenCredentialsAreValid() throws IOException {
        context
            .arrangeExpectedValues(true)
            .configureLogin(true)
            .configureEnterpriseClient();
        Optional<String> actualToken = context.getEnterpriseClient().login("wolf359", "pa55word");
        assertEquals(context.getExpectedAuthToken(), actualToken.get());
    }
    @Test
    void loginTokenNotPresentWhenCredentialsAreInvalid() throws IOException {
        context
            .arrangeExpectedValues(false)
            .configureLogin(false)
            .configureEnterpriseClient();
        assertFalse(context.getEnterpriseClient().login("wolf359", "pa55word").isPresent());
    }

    @Test
    void getEngineGroupIdForNameIsPresentWhenNameIsFound() throws IOException {
        context
                .arrangeExpectedValues()
                .configureGetAllEngineGroups(true)
                .configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient().getEngineGroupIdFromName(context.getExpectedAuthToken(), context.getFirstEngineName());

        assertTrue(engineId.isPresent());
    }
    @Test
    void getEngineGroupIdForNameCorrectResultReturnedWhenNameIsFound() throws IOException {
        context
                .arrangeExpectedValues()
                .configureGetAllEngineGroups(true)
                .configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient().getEngineGroupIdFromName(context.getExpectedAuthToken(), context.getFirstEngineName());

        assertEquals(context.getFirstEngineId(), engineId.get());
    }
    @Test
    void getEngineGroupIdForNameIsNotPresentWhenNameIsNotFound() throws IOException {
        context
                .arrangeExpectedValues()
                .configureGetAllEngineGroups(true)
                .configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient().getEngineGroupIdFromName(context.getExpectedAuthToken(), "nameNotFound");

        assertFalse(engineId.isPresent());
    }
    @Test
    void getEngineGroupIdForNameIsNotPresentWhenApiCallFails() throws IOException {
        context
                .arrangeExpectedValues()
                .configureGetAllEngineGroups(false)
                .configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient().getEngineGroupIdFromName(context.getExpectedAuthToken(), context.getFirstEngineName());

        assertFalse(engineId.isPresent());
    }
    @Test
    void getEngineGroupNamesForClientIsPresent() throws IOException {
        context
                .arrangeExpectedValues()
                .configureGetEngineGroupsForClient(true)
                .configureEnterpriseClient();

        Optional<String[]> engineGroupNames = context.getEnterpriseClient().getEngineGroupNamesForClient(context.getExpectedAuthToken());

        assertTrue(engineGroupNames.isPresent());
    }
    @Test
    void getEngineGroupNamesForClientCorrectResultReturned() throws IOException {
        context
                .arrangeExpectedValues()
                .configureGetEngineGroupsForClient(true)
                .configureEnterpriseClient();

        Optional<String[]> engineGroupNames = context.getEnterpriseClient().getEngineGroupNamesForClient(context.getExpectedAuthToken());

        List<String> expected = Arrays.asList(context.getEngineGroupNames());
        List<String> actual = Arrays.asList(engineGroupNames.get());
        Collections.sort(expected);
        Collections.sort(actual);
        assertArrayEquals(expected.toArray(), actual.toArray());
    }
    @Test
    void getEngineGroupNamesForClientIsNotPresentWhenApiCallFails() throws IOException {
        context
                .arrangeExpectedValues()
                .configureGetEngineGroupsForClient(false)
                .configureEnterpriseClient();

        Optional<String[]> engineGroupNames = context.getEnterpriseClient().getEngineGroupNamesForClient(context.getExpectedAuthToken());

        assertFalse(engineGroupNames.isPresent());
    }

    @Test
    void runScanConfigByNameIsSuccessWhenConfigFoundAndScanCreated() throws IOException {
        context
            .arrangeExpectedValues()
            .configureGetConfigs(true)
            .configureRunScanByConfigId(true)
            .configureEnterpriseClient();

        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(), context.getConfigName());

        assertTrue(scanResult.isSuccess());
    }
    @Test
    void runScanConfigByNameHasScanIdWhenConfigFoundAndScanCreated() throws IOException {
        context
            .arrangeExpectedValues(true)
            .configureGetConfigs(true)
            .configureRunScanByConfigId(true)
            .configureEnterpriseClient();
        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(), context.getConfigName());

        assertEquals(context.getExpectedScanId(), scanResult.getScanId());
    }
    @Test
    void runScanConfigByNameIsFailWhenConfigNotFound() throws IOException {
        context
                .arrangeExpectedValues(true)
                .configureGetConfigs(false)
                .configureRunScanByConfigId(true)
                .configureEnterpriseClient();

        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(), context.getConfigName());

        assertFalse(scanResult.isSuccess());
    }
    @Test
    void runScanConfigByNameIsFailWhenConfigFoundButRunFails() throws IOException {
        context
            .arrangeExpectedValues(true)
            .configureGetConfigs(true)
            .configureRunScanByConfigId(false)
            .configureEnterpriseClient();

        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(), context.getConfigName());

        assertFalse(scanResult.isSuccess());
    }
    @Test
    void getScanStatusIsPresentWhenApiCallSuceeds() throws IOException {
        context
            .arrangeExpectedValues()
            .configureGetScanStatus(true)
            .configureEnterpriseClient();

        Optional<String> status = context.getEnterpriseClient().getScanStatus(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertTrue(status.isPresent());
    }
    @Test
    void isScanFinishedIsTrueWhenResultAndIsSuccessAreTrue() throws IOException {
        context
            .arrangeExpectedValues()
            .configureIsScanFinished(true, true, true)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertTrue(isFinished);
    }
    @Test
    void isScanFinishedIsFalseWhenResultIsFalseAndIsSuccessIsTrue() throws IOException {
        context
            .arrangeExpectedValues()
            .configureIsScanFinished(true, true, false)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertFalse(isFinished);
    }
    @Test
    void isScanFinishedIsFalseWhenResultIsTrueAndIsSuccessIsFalse() throws IOException {
        context
            .arrangeExpectedValues()
            .configureIsScanFinished(true, false, true)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertFalse(isFinished);
    }
    @Test
    void isScanFinishedIsFalseWhenApiCallFails() throws IOException {
        context
            .arrangeExpectedValues()
            .configureIsScanFinished(false, true, true)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertFalse(isFinished);
    }
    @Test
    void hasReportIsTrueWhenResultAndIsSuccessAreTrue() throws IOException {
        context
            .arrangeExpectedValues()
            .configureHasReport(true, true, true)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertTrue(isFinished);
    }
    @Test
    void hasReportIsFalseWhenResultIsFalseAndIsSuccessIsTrue() throws IOException {
        context
            .arrangeExpectedValues()
            .configureHasReport(true, true, false)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertFalse(isFinished);
    }
    @Test
    void hasReportIsFalseWhenResultIsTrueAndIsSuccessIsFalse() throws IOException {
        context
            .arrangeExpectedValues()
            .configureHasReport(true, false, true)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertFalse(isFinished);
    }
    @Test
    void hasReportIsFalseWhenApiCallFails() throws IOException {
        context
            .arrangeExpectedValues()
            .configureHasReport(false, true, true)
            .configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertFalse(isFinished);
    }

    @Test
    void getConfigsIsPresentWhenApiCallSucceeds() throws IOException {
        context
            .arrangeExpectedValues()
            .configureGetConfigs(true)
            .configureEnterpriseClient();

        Optional<String[]> configs = context.getEnterpriseClient().getConfigNames(context.getExpectedAuthToken());

        assertTrue(configs.isPresent());
    }
    @Test
    void getConfigsIsNotPresentWhenApiCallFails() throws IOException {
        context
            .arrangeExpectedValues()
            .configureGetConfigs(false)
            .configureEnterpriseClient();

        Optional<String[]> configs = context.getEnterpriseClient().getConfigNames(context.getExpectedAuthToken());

        assertFalse(configs.isPresent());
    }

}
