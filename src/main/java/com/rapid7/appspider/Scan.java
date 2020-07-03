/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
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

    private final StandardEnterpriseClient client;
    private final ScanSettings settings;
    private final LoggerFacade log;
    private Optional<String> id;

    public Scan(StandardEnterpriseClient client, ScanSettings settings, LoggerFacade log) {
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

    public boolean process(String username, String password) throws InterruptedException {
        Optional<String> maybeAuthToken = client.login(username, password);
        if (!maybeAuthToken.isPresent()) {
            log.println(UNAUTHORIZED_ERROR);
            return false;
        }
        String authToken = maybeAuthToken.get();
        if (!createScanBeforeRunIfNeeded(authToken))
            return false;

        ScanResult runResult = client.runScanByConfigName(authToken, settings.getConfigName());
        if (!runResult.isSuccess()) {
            log.println(String.format("Error: Response from %s came back not successful",  client.getUrl()));
        } else {
            log.println(String.format("Scan for successfully started.",  settings.getConfigName()));
        }

        if (!settings.getGenerateReport()) {
            log.println("Continuing the build without generating the report.");
            return true;
        }

        waitForScanCompletion(runResult.getScanId(), username, password);

        maybeAuthToken = client.login(username, password);
        if (!maybeAuthToken.isPresent()) {
            log.println(UNAUTHORIZED_ERROR);
            return false;
        }
        authToken = maybeAuthToken.get();

        if (!client.hasReport(authToken, runResult.getScanId())) {
            log.println(String.format("No reports for this scan: %s", runResult.getScanId()));
        }

        String status = client.getScanStatus(authToken, runResult.getScanId()).orElse(FAILED_SCAN);
        log.println(status.matches(SUCCESSFUL_SCAN)
            ? "Finished scanning!"
            : String.format("Scan was complete but was not successful. Status was '%s'", status));
        return true;
    }

    private boolean createScanBeforeRunIfNeeded(String authToken) {
        final String newConfigName = settings.getNewConfigName();
        final String newConfigUrl = settings.getNewConfigUrl();
        final boolean isNewConfig = (!(Objects.isNull(newConfigName) || newConfigName.isEmpty()) && !(Objects.isNull(newConfigUrl) || newConfigUrl.isEmpty()));
        if (!isNewConfig) // local not really needed but is provided to clarify the meaning of these conditions
            return true; // creation not needed

        log.println("Value of Scan Config Name: " + newConfigName);
        log.println("Value of Scan Config URL: " + newConfigUrl);
        log.println("Value of Scan Config Engine Group name: " + settings.getScanConfigEngineGroupName());

        Optional<String> engineGroupId = client.getEngineGroupIdFromName(authToken, settings.getScanConfigEngineGroupName());
        if (!engineGroupId.isPresent()) {
            log.println(String.format("no engine group matching %s was found.", settings.getScanConfigEngineGroupName()));
            return false;
        }

        if  (client.saveConfig(authToken, newConfigName, newConfigUrl, engineGroupId.get())) {
            settings.setConfigName(settings.getNewConfigName());
            log.println(String.format("Successfully created the scan config %s", newConfigName));

            settings.resetNewConfigValues();
            return true;
        } else {
            log.println(String.format("An error occurred while attempting to save %s.", newConfigName));
            return false;
        }
    }

    private void waitForScanCompletion(String scanId, String username, String password) throws InterruptedException {
        String scanStatus;
        try {
            do {
                TimeUnit.SECONDS.sleep(settings.getStatusPollTime());
                scanStatus = getStatus(scanId, username, password)
                        .orElseGet(this::failedStatusRequest);
                log.println("Scan status: [" + scanStatus +"]");

            } while (!scanStatus.matches(FINISHED_SCANNING));

        } catch (InterruptedException e) {
            log.println("Unexpected error occured: " + e.toString());
            throw e;
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
