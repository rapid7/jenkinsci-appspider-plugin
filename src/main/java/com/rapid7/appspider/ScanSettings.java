/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

/**
 * Storage class for scan settings provided by Jenkins
 */
public class ScanSettings {

    private final String configName;
    private final String reportName;
    private final boolean enableScan;
    private final boolean generateReport;
    private final String scanConfigName;
    private final String scanConfigUrl;
    private final String scanConfigEngineGroupName;
    private final int statusPollTime;

    /**
     * Constructs a DTO style container object storing scan settings from Jenkins
     * @param configName name of the scan config to run
     * @param reportName name of the report
     * @param enableScan when to enable a new scan
     * @param generateReport whether to generate a report at the end of the scan
     * @param scanConfigName name of the scan configuration to run (as known to AppSpider Enterprise)
     * @param scanConfigUrl URL of the scan configuration
     * @param scanConfigEngineGroupName name of the engine group to run the scan under
     */
    public ScanSettings(String configName, String reportName,
                 Boolean enableScan, Boolean generateReport,
                 String scanConfigName, String scanConfigUrl,
                 String scanConfigEngineGroupName) {

        this.configName = configName;
        this.reportName = reportName;
        this.enableScan = enableScan;
        this.generateReport = generateReport;
        this.scanConfigName = scanConfigName;
        this.scanConfigUrl = scanConfigUrl;
        this.scanConfigEngineGroupName = scanConfigEngineGroupName;
        this.statusPollTime = 90; // TODO: move this to configuration
    }

    public String getConfigName() {
        return configName;
    }
    public String getReportName() {
        return reportName;
    }
    public boolean getEnableScan() {
        return enableScan;
    }
    public boolean getGenerateReport() {
        return generateReport;
    }
    public String getScanConfigName() {
        return scanConfigName;
    }
    public String getScanConfigEngineGroupName() {
        return scanConfigEngineGroupName;
    }

    public String getScanConfigUrl() {
        return scanConfigUrl;
    }

    public int getStatusPollTime() {
        return statusPollTime;
    }
}
