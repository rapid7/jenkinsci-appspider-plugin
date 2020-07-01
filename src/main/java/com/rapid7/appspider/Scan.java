package com.rapid7.appspider;

class Scan {
    private final EnterpriseClient client;
    private final ScanSettings settings;
    private final LoggerFacade logger;

    Scan(EnterpriseClient client, ScanSettings settings, LoggerFacade logger) {
        this.client = client;
        this.settings = settings;
        this.logger = logger;
    }



}
