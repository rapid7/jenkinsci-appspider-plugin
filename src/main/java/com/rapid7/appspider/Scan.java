/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ScanResult;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class Scan {
    private static final String SUCCESSFUL_SCAN = "Completed|Stopped";
    private static final String UNSUCCESSFUL_SCAN = "ReportError";
    private static final String FAILED_SCAN = "Failed";
    private static final String FINISHED_SCANNING = SUCCESSFUL_SCAN + "|" + UNSUCCESSFUL_SCAN + "|" + FAILED_SCAN;
    private static final String UNAUTHORIZED_ERROR = "Unauthorized, please verify credentials and try again.";

    private final EnterpriseClient client;
    private final ScanSettings settings;
    private final LoggerFacade log;
    private Optional<String> id;

    public Scan(EnterpriseClient client, ScanSettings settings, LoggerFacade log) {
        if (Objects.isNull(client))
            throw new IllegalArgumentException("client cannot be null");
        if (Objects.isNull(settings))
            throw new IllegalArgumentException("settings cannot be null");
        if (Objects.isNull(log))
            throw new IllegalArgumentException("log cannot be null");
        this.client = client;
        this.settings = settings;
        this.log = log;
    }

    /**
     * returns the current scan id, this will be empty until process has been called
     * @return the current scan id
     */
    public Optional<String> getId() {
        return id;
    }

    public boolean process(String username, String password) {
        Optional<String> authToken = client.login(username, password);
        if (!authToken.isPresent()) {
            log.println(UNAUTHORIZED_ERROR);
            return false;
        }
        createScanBeforeRunIfNeeded();

        ScanResult runResult = client.runScanByConfigName(authToken.get(), settings.getConfigName());
        if (!runResult.isSuccess()) {
            log.println("Error: Response from " + client.getUrl() + " came back not successful");
        }
        id = Optional.of(runResult.getScanId());

        if (!settings.getGenerateReport()) {
            log.println("Continuing the build without generating the report.");
            return true;
        }

        if (!waitForScanCompletion(runResult.getScanId(), username, password))
            return false;

        authToken = client.login(username, password);
        if (!authToken.isPresent()) {
            log.println(UNAUTHORIZED_ERROR);
            return false;
        }

        if (!client.hasReport(authToken.get(), runResult.getScanId())) {
            log.println("No reports for this scan: " + runResult.getScanId());
        }

        String status = client.getScanStatus(authToken.get(), runResult.getScanId()).orElse(FAILED_SCAN);
        if (status.matches(SUCCESSFUL_SCAN)) {
            log.println("Scan was complete but was not successful. Status was '" + status + "'");
            return true;
        }

        log.println("Finished scanning!");
        return true;
    }

    private void createScanBeforeRunIfNeeded() {
         if ((Objects.isNull(settings.getConfigName()) || settings.getConfigName().isEmpty()) &&
             (Objects.isNull(settings.getScanConfigUrl()) || settings.getScanConfigUrl().isEmpty())) {
             return;
         }

        log.println("Value of Scan Config Name: " + settings.getConfigName());
        log.println("Value of Scan Config URL: " + settings.getScanConfigUrl());
        log.println("Value of Scan Config Engine Group name: " + settings.getScanConfigEngineGroupName());
    }

    private boolean waitForScanCompletion(String scanId, String username, String password) {
        String scanStatus;
        try {
            do {
                TimeUnit.SECONDS.sleep(settings.getStatusPollTime());
                scanStatus = getStatus(scanId, username, password)
                        .orElseGet(this::failedStatusRequest);
                log.println("Scan status: [" + scanStatus +"]");

            } while (!scanStatus.matches(FINISHED_SCANNING));

            return true;

        } catch (InterruptedException e) {
            log.println("Unexpected error occured: " + e.toString());
            return false;
        }
    }
    private Optional<String> getStatus(String scanId, String username, String password) {
        Optional<String> authToken = client.login(username, password);
        if (!authToken.isPresent()) {
            log.println(UNAUTHORIZED_ERROR);
            return Optional.empty();
        }
        return client.getScanStatus(authToken.get(), scanId);
    }

    private String failedStatusRequest() {
       log.println("Unexpected error occurred getting current scan status");
       return FAILED_SCAN;
    }

}
