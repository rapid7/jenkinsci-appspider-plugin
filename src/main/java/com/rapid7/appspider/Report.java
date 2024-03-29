/*
 * Copyright © 2003 - 2021 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import hudson.FilePath;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

import com.rapid7.appspider.models.AuthenticationModel;

public class Report {

    private static final int BUFFER_SIZE = 4096;

    private final EnterpriseClient client;
    private final ScanSettings settings;
    private final LoggerFacade log;

    public static Report createInstanceOrThrow(EnterpriseClient client, ScanSettings settings, LoggerFacade log) {
        if (Objects.isNull(client))
            throw new IllegalArgumentException("client cannot be null");
        if (Objects.isNull(settings))
            throw new IllegalArgumentException("settings cannot be null");
        if (Objects.isNull(log))
            throw new IllegalArgumentException("log cannot be null");
        return new Report(client, settings, log);
    }

    private Report(EnterpriseClient client, ScanSettings settings, LoggerFacade log) {
        this.client = client;
        this.settings = settings;
        this.log = log;
    }

    public boolean saveReport(AuthenticationModel authModel, String scanId, FilePath directory) {

        if (Objects.isNull(directory))
            throw new IllegalArgumentException("directory cannot be null or empty");

        // FilePath.toString() is deprecated with no alternative provided
        String reportFolder = Paths.get("" + directory.getParent(), directory.getBaseName()).toString();

        log.println("Generating xml report and downloading report zip file to:" + directory);
        Optional<String> maybeAuthToken = client.login(authModel);
        if (maybeAuthToken.isEmpty()) {
            log.println("Unauthorized: unable to retrieve vulnerabilities summary and report.zip");
            return false;
        }
        String authToken = maybeAuthToken.get();

        String dateTimeStamp = "_" + getNowAsFormattedString();
        Path vulnerabilitiesFilename = Paths.get(reportFolder, settings.getReportName() + dateTimeStamp + ".xml");
        Path reportZipFilename = Paths.get(reportFolder, settings.getReportName() + dateTimeStamp +  ".zip");

        return saveVulnerabilities(authToken, scanId, vulnerabilitiesFilename) &&
                saveReportZip(authToken, scanId, reportZipFilename);
    }

    private boolean saveVulnerabilities(String authToken, String scanId, Path file) {

        Optional<String> xml = client.getVulnerabilitiesSummaryXml(authToken, scanId);
        if (xml.isEmpty()) {
            log.println("Unable to retrieve vulnerabilities summary.");
            return false;
        }
        return saveXmlFile(file, xml.get());
    }
    private boolean saveReportZip(String authToken, String scanId, Path file) {
        return client.getReportZip(authToken, scanId)
            .map(inputStream -> saveInputStreamToFile(file, inputStream))
            .orElse(false);
    }
    private boolean saveXmlFile(Path file, String content) {
        try {
            if (!Files.exists(file) )
                Files.createFile(file);

            try (BufferedWriter writer = Files.newBufferedWriter(file, StandardCharsets.UTF_8)) {
                writer.write(content);
                writer.flush();
            }
            return true;
        } catch (IOException e) {
            log.println(e.toString());
            return false;
        }
    }
    private boolean saveInputStreamToFile(Path file, InputStream inputStream) {
        if (Objects.isNull(inputStream))
            return false;

        try {
            if (!Files.exists(file) )
                Files.createFile(file);

            try (InputStream bufferedInput = new BufferedInputStream(inputStream);
                 OutputStream outputStream = Files.newOutputStream(file)) {

                byte[] buffer = new byte[BUFFER_SIZE];
                int bytesRead = 0;
                while ((bytesRead = bufferedInput.read(buffer)) != -1) {
                   outputStream.write(buffer, 0, bytesRead);
                }
                outputStream.flush();
                return true;
            }
        } catch (IOException e) {
            log.println(e.toString());
            return false;
        }
    }
    private String getNowAsFormattedString() {
        LocalDateTime now = Instant.now()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("");
        return now.format(formatter);
    }
}
