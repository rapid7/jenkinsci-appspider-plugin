/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.jenkinspider;

import com.rapid7.appspider.LoggerFacade;
import hudson.model.BuildListener;

import java.io.PrintStream;
import java.util.logging.Level;

class PrintStreamLoggerFacade implements LoggerFacade {

    private final PrintStream stream;
    private final java.util.logging.Logger logger;

    public PrintStreamLoggerFacade(BuildListener listener) {
        this.stream = listener.getLogger();
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
    @Override
    public void verbose(String message) {
        logger.log(Level.FINE, message);
    }

    @Override
    public boolean isInfoEnabled() {
        return logger.isLoggable(Level.INFO);
    }

    @Override
    public boolean isWarnEnabled() {
        return logger.isLoggable(Level.WARNING);
    }

    @Override
    public boolean isSevereEnabled() {
        return logger.isLoggable(Level.SEVERE);
    }

    @Override
    public boolean isVerboseEnabled() {
        return logger.isLoggable(Level.ALL);
    }
}
