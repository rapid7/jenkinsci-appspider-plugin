/*
 * Copyright © 2003 - 2021 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import com.rapid7.appspider.datatransferobjects.ScanResult;
import com.rapid7.appspider.models.AuthenticationModel;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

public class DastScan {
    private static final String SUCCESSFUL_SCAN = "Completed|Stopped";
    private static final String UNSUCCESSFUL_SCAN = "ReportError";
    private static final String FAILED_SCAN = "Failed";
    private static final String FINISHED_SCANNING = SUCCESSFUL_SCAN + "|" + UNSUCCESSFUL_SCAN + "|" + FAILED_SCAN;
    private static final String UNAUTHORIZED_ERROR = "Unauthorized, please verify credentials and try again.";

    private final EnterpriseClient client;
    private ScanSettings settings;
    private final LoggerFacade log;
    private Optional<String> id;

    public static DastScan createInstanceOrThrow(EnterpriseClient client, ScanSettings settings, LoggerFacade log) {
        if (Objects.isNull(client))
            throw new IllegalArgumentException("client cannot be null");
        if (Objects.isNull(settings))
            throw new IllegalArgumentException("settings cannot be null");
        if (Objects.isNull(log))
            throw new IllegalArgumentException("log cannot be null");
        return new DastScan(client, settings, log);
    }

    private DastScan(EnterpriseClient client, ScanSettings settings, LoggerFacade log) {
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

    public boolean process(AuthenticationModel authModel) throws InterruptedException {
        Optional<String> maybeAuthToken = client.login(authModel);
        if (maybeAuthToken.isEmpty()) {
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
            log.println(String.format("Scan for '%s' successfully started.",  settings.getConfigName()));
        }
        id = Optional.of(runResult.getScanId());

        if (!settings.getGenerateReport()) {
            log.println("Continuing the build without generating the report.");
            return true;
        }

        waitForScanCompletion(runResult.getScanId(), authModel);

        maybeAuthToken = client.login(authModel);
        if (maybeAuthToken.isEmpty()) {
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
        if (engineGroupId.isEmpty()) {
            log.println(String.format("no engine group matching %s was found.", settings.getScanConfigEngineGroupName()));
            return false;
        }

        URL newScanConfigTarget;
        try {
            newScanConfigTarget = new URL(newConfigUrl);
        } catch (MalformedURLException e) {
            log.println(String.format("Invalid scan targat '%s', unable to save configuration", newConfigUrl));
            log.severe(e.getMessage());
            return false;
        }

        if  (client.saveConfig(authToken, newConfigName, newScanConfigTarget, engineGroupId.get())) {
            settings = settings
                .withConfigName(settings.getNewConfigName())
                .withEmptyConfigValues();
            log.println(String.format("Successfully created the scan config %s", newConfigName));
            return true;
        } else {
            log.println(String.format("An error occurred while attempting to save %s.", newConfigName));
            return false;
        }
    }

    private void waitForScanCompletion(String scanId, AuthenticationModel authModel) throws InterruptedException {
        String scanStatus;
        try {
            do {
                TimeUnit.SECONDS.sleep(settings.getStatusPollTime());
                scanStatus = getStatus(scanId, authModel)
                        .orElseGet(this::failedStatusRequest);
                log.println("Scan status: [" + scanStatus +"]");

            } while (!scanStatus.matches(FINISHED_SCANNING));

        } catch (InterruptedException e) {
            log.println("Unexpected error occurred: " + e.toString());
            throw e;
        }
    }
    private Optional<String> getStatus(String scanId, AuthenticationModel authModel) {
        Optional<String> authToken = client.login(authModel);
        if (authToken.isEmpty()) {
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
