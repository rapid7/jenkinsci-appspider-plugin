package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ClientIdNamePair;
import com.rapid7.appspider.datatransferobjects.ScanResult;
import com.rapid7.appspider.models.AuthenticationModel;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class EnterpriseRestClientTest {

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
        context.arrangeExpectedValues(true).configureLogin(true).configureEnterpriseClient();
        assertTrue(context.getEnterpriseClient().testAuthentication(new AuthenticationModel("wolf359", "pa55word")));
    }

    @Test
    void testAuthenticationReturnsFalseWhenCredentialsAreInvalid() throws IOException {
        context.arrangeExpectedValues(false).configureLogin(false).configureEnterpriseClient();

        // act and assert
        assertFalse(context.getEnterpriseClient().testAuthentication(new AuthenticationModel("wolf359", "pa55word")));
    }

    @Test
    void loginHasTokenWhenCredentialsAreValid() throws IOException {
        context.arrangeExpectedValues(true).configureLogin(true).configureEnterpriseClient();
        assertTrue(context.getEnterpriseClient().login(new AuthenticationModel("wolf359", "pa55word")).isPresent());
    }

    @Test
    void loginTokenHasExpectedValueWhenCredentialsAreValid() throws IOException {
        context.arrangeExpectedValues(true).configureLogin(true).configureEnterpriseClient();
        Optional<String> actualToken = context.getEnterpriseClient().login(new AuthenticationModel("wolf359", "pa55word"));
        assertEquals(context.getExpectedAuthToken(), actualToken.get());
    }

    @Test
    void loginTokenNotPresentWhenCredentialsAreInvalid() throws IOException {
        context.arrangeExpectedValues(false).configureLogin(false).configureEnterpriseClient();
        assertFalse(context.getEnterpriseClient().login(new AuthenticationModel("wolf359", "pa55word")).isPresent());
    }

    @Test
    void getEngineGroupIdForNameIsPresentWhenNameIsFound() throws IOException {
        context.arrangeExpectedValues().configureGetAllEngineGroups(true).configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient()
                .getEngineGroupIdFromName(context.getExpectedAuthToken(), context.getFirstEngineName());

        assertTrue(engineId.isPresent());
    }

    @Test
    void getEngineGroupIdForNameCorrectResultReturnedWhenNameIsFound() throws IOException {
        context.arrangeExpectedValues().configureGetAllEngineGroups(true).configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient()
                .getEngineGroupIdFromName(context.getExpectedAuthToken(), context.getFirstEngineName());

        assertEquals(context.getFirstEngineId(), engineId.get());
    }

    @Test
    void getEngineGroupIdForNameIsNotPresentWhenNameIsNotFound() throws IOException {
        context.arrangeExpectedValues().configureGetAllEngineGroups(true).configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient()
                .getEngineGroupIdFromName(context.getExpectedAuthToken(), "nameNotFound");

        assertFalse(engineId.isPresent());
    }

    @Test
    void getEngineGroupIdForNameIsNotPresentWhenApiCallFails() throws IOException {
        context.arrangeExpectedValues().configureGetAllEngineGroups(false).configureEnterpriseClient();

        Optional<String> engineId = context.getEnterpriseClient()
                .getEngineGroupIdFromName(context.getExpectedAuthToken(), context.getFirstEngineName());

        assertFalse(engineId.isPresent());
    }

    @Test
    void getEngineGroupNamesForClientIsPresent() throws IOException {
        context.arrangeExpectedValues().configureGetEngineGroupsForClient(true).configureEnterpriseClient();

        Optional<String[]> engineGroupNames = context.getEnterpriseClient()
                .getEngineGroupNamesForClient(context.getExpectedAuthToken());

        assertTrue(engineGroupNames.isPresent());
    }

    @Test
    void getEngineGroupNamesForClientCorrectResultReturned() throws IOException {
        context.arrangeExpectedValues().configureGetEngineGroupsForClient(true).configureEnterpriseClient();

        Optional<String[]> engineGroupNames = context.getEnterpriseClient()
                .getEngineGroupNamesForClient(context.getExpectedAuthToken());

        List<String> expected = Arrays.asList(context.getEngineGroupNames());
        List<String> actual = Arrays.asList(engineGroupNames.get());
        Collections.sort(expected);
        Collections.sort(actual);
        assertArrayEquals(expected.toArray(), actual.toArray());
    }

    @Test
    void getEngineGroupNamesForClientIsNotPresentWhenApiCallFails() throws IOException {
        context.arrangeExpectedValues().configureGetEngineGroupsForClient(false).configureEnterpriseClient();

        Optional<String[]> engineGroupNames = context.getEnterpriseClient()
                .getEngineGroupNamesForClient(context.getExpectedAuthToken());

        assertFalse(engineGroupNames.isPresent());
    }

    @Test
    void runScanConfigByNameIsSuccessWhenConfigFoundAndScanCreated() throws IOException {
        context.arrangeExpectedValues().configureGetConfigs(true).configureRunScanByConfigId(true)
                .configureEnterpriseClient();

        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(),
                context.getConfigName());

        assertTrue(scanResult.isSuccess());
    }

    @Test
    void runScanConfigByNameHasScanIdWhenConfigFoundAndScanCreated() throws IOException {
        context.arrangeExpectedValues(true).configureGetConfigs(true).configureRunScanByConfigId(true)
                .configureEnterpriseClient();
        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(),
                context.getConfigName());

        assertEquals(context.getExpectedScanId(), scanResult.getScanId());
    }

    @Test
    void runScanConfigByNameIsFailWhenConfigNotFound() throws IOException {
        context.arrangeExpectedValues(true).configureGetConfigs(false).configureRunScanByConfigId(true)
                .configureEnterpriseClient();

        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(),
                context.getConfigName());

        assertFalse(scanResult.isSuccess());
    }

    @Test
    void runScanConfigByNameIsFailWhenConfigFoundButRunFails() throws IOException {
        context.arrangeExpectedValues(true).configureGetConfigs(true).configureRunScanByConfigId(false)
                .configureEnterpriseClient();

        ScanResult scanResult = context.getEnterpriseClient().runScanByConfigName(context.getExpectedAuthToken(),
                context.getConfigName());

        assertFalse(scanResult.isSuccess());
    }

    @Test
    void getScanStatusIsPresentWhenApiCallSuceeds() throws IOException {
        context.arrangeExpectedValues().configureGetScanStatus(true).configureEnterpriseClient();

        Optional<String> status = context.getEnterpriseClient().getScanStatus(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertTrue(status.isPresent());
    }

    @Test
    void isScanFinishedIsTrueWhenResultAndIsSuccessAreTrue() throws IOException {
        context.arrangeExpectedValues().configureIsScanFinished(true, true, true).configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertTrue(isFinished);
    }

    @Test
    void isScanFinishedIsFalseWhenResultIsFalseAndIsSuccessIsTrue() throws IOException {
        context.arrangeExpectedValues().configureIsScanFinished(true, true, false).configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertFalse(isFinished);
    }

    @Test
    void isScanFinishedIsFalseWhenResultIsTrueAndIsSuccessIsFalse() throws IOException {
        context.arrangeExpectedValues().configureIsScanFinished(true, false, true).configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertFalse(isFinished);
    }

    @Test
    void isScanFinishedIsFalseWhenApiCallFails() throws IOException {
        context.arrangeExpectedValues().configureIsScanFinished(false, true, true).configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().isScanFinished(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertFalse(isFinished);
    }

    @Test
    void hasReportIsTrueWhenResultAndIsSuccessAreTrue() throws IOException {
        context.arrangeExpectedValues().configureHasReport(true, true, true).configureEnterpriseClient();

        boolean isFinished = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertTrue(isFinished);
    }

    @Test
    void hasReportIsFalseWhenResultIsFalseAndIsSuccessIsTrue() throws IOException {
        context.arrangeExpectedValues().configureHasReport(true, true, false).configureEnterpriseClient();

        boolean hasReport = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertFalse(hasReport);
    }

    @Test
    void hasReportIsFalseWhenResultIsTrueAndIsSuccessIsFalse() throws IOException {
        context.arrangeExpectedValues().configureHasReport(true, false, true).configureEnterpriseClient();

        boolean hasReport = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertFalse(hasReport);
    }

    @Test
    void hasReportIsFalseWhenApiCallFails() throws IOException {
        context.arrangeExpectedValues().configureHasReport(false, true, true).configureEnterpriseClient();

        boolean hasReport = context.getEnterpriseClient().hasReport(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertFalse(hasReport);
    }

    @Test
    void getConfigsIsPresentWhenApiCallSucceeds() throws IOException {
        context.arrangeExpectedValues().configureGetConfigs(true).configureEnterpriseClient();

        Optional<String[]> configs = context.getEnterpriseClient().getConfigNames(context.getExpectedAuthToken());

        assertTrue(configs.isPresent());
    }

    @Test
    void getConfigsIsNotPresentWhenApiCallFails() throws IOException {
        context.arrangeExpectedValues().configureGetConfigs(false).configureEnterpriseClient();

        Optional<String[]> configs = context.getEnterpriseClient().getConfigNames(context.getExpectedAuthToken());

        assertFalse(configs.isPresent());
    }

    @Test
    void saveConfigIsTrueWhenResultIsSuccess() throws IOException {
        context.arrangeExpectedValues().configureSaveConfig(true, true).configureEnterpriseClient();

        boolean saved = context.getEnterpriseClient().saveConfig(context.getExpectedAuthToken(),
                "name" + UUID.randomUUID().toString(), new URL("http://www.webscantest.com"),
                "engine" + UUID.randomUUID().toString());

        assertTrue(saved);
    }

    @Test
    void saveConfigIsFalseWhenResultIsFail() throws IOException {
        context.arrangeExpectedValues().configureSaveConfig(true, false).configureEnterpriseClient();

        boolean saved = context.getEnterpriseClient().saveConfig(context.getExpectedAuthToken(),
                "name" + UUID.randomUUID().toString(), new URL("http://www.webscantest.com"),
                "engine" + UUID.randomUUID().toString());

        assertFalse(saved);
    }

    @Test
    void saveConfigIsFalseWhenApiCallFails() throws IOException {
        context.arrangeExpectedValues().configureSaveConfig(false, true).configureEnterpriseClient();

        boolean saved = context.getEnterpriseClient().saveConfig(context.getExpectedAuthToken(),
                "name" + UUID.randomUUID().toString(), new URL("http://www.webscantest.com"),
                "engine" + UUID.randomUUID().toString());

        assertFalse(saved);
    }

    @Test
    void getVulnerabilitySummaryXmlIsPresentWhenSuccessful() throws IOException {
        String xml = String.format("<?xml><vulns id=\"%s\"></vulns>", UUID.randomUUID());
        context.arrangeExpectedValues().configureGetVulnerabilitiesSummaryXml(true, xml).configureEnterpriseClient();

        Optional<String> actualXml = context.getEnterpriseClient()
                .getVulnerabilitiesSummaryXml(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertTrue(actualXml.isPresent());
    }

    @Test
    void getVulnerabilitySummaryXmlCorrectResponseWhenSuccessful() throws IOException {
        String xml = String.format("<?xml><vulns id=\"%s\"></vulns>", UUID.randomUUID());
        context.arrangeExpectedValues().configureGetVulnerabilitiesSummaryXml(true, xml).configureEnterpriseClient();

        Optional<String> actualXml = context.getEnterpriseClient()
                .getVulnerabilitiesSummaryXml(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertEquals(xml, actualXml.orElse("wrong"));
    }

    @Test
    void getVulnerabilitySummaryXmlIsNotPresentWhenFails() throws IOException {
        String xml = String.format("<?xml><vulns id=\"%s\"></vulns>", UUID.randomUUID());
        context.arrangeExpectedValues().configureGetVulnerabilitiesSummaryXml(false, xml).configureEnterpriseClient();

        Optional<String> actualXml = context.getEnterpriseClient()
                .getVulnerabilitiesSummaryXml(context.getExpectedAuthToken(), context.getExpectedScanId());

        assertFalse(actualXml.isPresent());
    }

    @Test
    void getReportZipIsPresentWhenSuccessful() throws IOException {
        String content = UUID.randomUUID().toString();
        byte[] encodedContent = Base64.getEncoder().encode(content.getBytes());
        context.arrangeExpectedValues().configureGetReportZip(true, encodedContent).configureEnterpriseClient();

        Optional<InputStream> inputStream = context.getEnterpriseClient().getReportZip(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertTrue(inputStream.isPresent());
        inputStream.get().close();
    }

    @Test
    void getReportZipCorrectResultValueSuccessful() throws IOException {
        String content = UUID.randomUUID().toString();
        byte[] encodedContent = Base64.getEncoder().encode(content.getBytes());
        context.arrangeExpectedValues().configureGetReportZip(true, encodedContent).configureEnterpriseClient();

        Optional<InputStream> maybeInputStream = context.getEnterpriseClient()
                .getReportZip(context.getExpectedAuthToken(), context.getExpectedScanId());
        try (InputStream inputStream = maybeInputStream.orElseThrow(() -> new RuntimeException("test failure"))) {
            byte[] encodedResponse = new byte[inputStream.available()];

            assertEquals(inputStream.available(), inputStream.read(encodedResponse));
            String decoded = new String(Base64.getDecoder().decode(encodedResponse));
            assertEquals(content, decoded);
        }
    }

    @Test
    void getReportZipIsNotPresentWhenFails() throws IOException {
        String content = UUID.randomUUID().toString();
        byte[] encodedContent = Base64.getEncoder().encode(content.getBytes());
        context.arrangeExpectedValues().configureGetReportZip(false, encodedContent).configureEnterpriseClient();

        Optional<InputStream> inputStream = context.getEnterpriseClient().getReportZip(context.getExpectedAuthToken(),
                context.getExpectedScanId());

        assertFalse(inputStream.isPresent());
    }

    @Test
    void getClientIdNamePairsIsPresentWhenSuccessful() throws IOException {
        context
            .arrangeExpectedValues()
            .configureGetClientIdNamePairs(true)
            .configureEnterpriseClient();

        Optional<List<ClientIdNamePair>> configs = context
            .getEnterpriseClient()
            .getClientNameIdPairs(context.getExpectedAuthToken());

        assertTrue(configs.isPresent());

    }

    @Test
    void getClientIdNamePairsIsNotPresentWhenFail() throws IOException {
        context
            .arrangeExpectedValues()
            .configureGetClientIdNamePairs(false)
            .configureEnterpriseClient();

        Optional<List<ClientIdNamePair>> configs = context
            .getEnterpriseClient()
            .getClientNameIdPairs(context.getExpectedAuthToken());

        assertFalse(configs.isPresent());

    }
}
