/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import java.io.PrintStream;
import java.util.logging.Level;

public class PrintStreamLoggerFacade implements LoggerFacade {

    private final PrintStream stream;
    private final java.util.logging.Logger logger;

    public PrintStreamLoggerFacade(PrintStream stream) {
        this.stream = stream;
        logger = java.util.logging.Logger.getLogger("appspider-plugin");
    }

    @Override
    public void println(String message) {
        stream.println(message);
    }
    @Override
    public void info(String message) {
        logger.log(Level.INFO, message);
    }
    @Override
    public void warn(String message) {
        logger.log(Level.WARNING, message);
    }
    @Override
    public void severe(String message) {
        logger.log(Level.SEVERE, message);
    }
}
