/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import hudson.FilePath;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.Optional;

public class Report {

    private static final int BUFFER_SIZE = 4096;

    private final EnterpriseClient client;
    private final ScanSettings settings;
    private final LoggerFacade log;

    public Report(EnterpriseClient client, ScanSettings settings, LoggerFacade log) {

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

    public boolean SaveReport(String username, String password, String scanId, FilePath directory) {

        if (Objects.isNull(directory))
            throw new IllegalArgumentException("directory cannot be null or empty");

        // FilePath.toString() is deprecated with no alternative provided
        String reportFolder = Paths.get("" + directory.getParent(), directory.getBaseName()).toString();

        log.println("Generating xml report to:" + directory);
        Optional<String> maybeAuthToken = client.login(username, password);
        if (!maybeAuthToken.isPresent()) {
            log.println("Unauthorized: unable to retrieve vulnerabilities summary and report.zip");
            return false;
        }
        String authToken = maybeAuthToken.get();

        String dateTimeStamp = "_" + getNowAsFormattedString();
        Path vulnerabiltiesFilename = Paths.get(reportFolder, settings.getReportName() + dateTimeStamp + ".xml");
        Path reportZipFilename = Paths.get(reportFolder, settings.getReportName() + dateTimeStamp +  ".zip");

        return SaveVulnerabilities(authToken, scanId, vulnerabiltiesFilename) &&
                SaveReportZip(authToken, scanId, reportZipFilename);
    }

    private boolean SaveVulnerabilities(String authToken, String scanId, Path file) {

        Optional<String> xml = client.getVulnerabilitiesSummaryXml(authToken, scanId);
        if (!xml.isPresent()) {
            log.println("Unable to retrieve vulnerabilities summary.");
            return false;
        }
        return SaveXmlFile(file, xml.get());
    }
    private boolean SaveReportZip(String authToken, String scanId, Path file) {
        return client.getReportZip(authToken, scanId)
            .map(inputStream -> SaveInputStreamToFile(file, inputStream))
            .orElse(false);
    }
    private boolean SaveXmlFile(Path file, String content) {
        try {
            if (!Files.exists(file) )
                file = Files.createFile(file);

            try (BufferedWriter writer = Files.newBufferedWriter(file)) {
                writer.write(content);
                writer.flush();
            }
            return true;
        } catch (IOException e) {
            log.println(e.toString());
            return false;
        }
    }
    private boolean SaveInputStreamToFile(Path file, InputStream inputStream) {
        if (Objects.isNull(inputStream))
            return false;

        try {
            if (!Files.exists(file) )
                file = Files.createFile(file);

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
