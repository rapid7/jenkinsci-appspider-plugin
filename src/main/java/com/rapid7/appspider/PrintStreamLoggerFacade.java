/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import java.io.PrintStream;

public class PrintStreamLoggerFacade implements LoggerFacade {

    private final PrintStream stream;

    public PrintStreamLoggerFacade(PrintStream stream) {
        this.stream = stream;
    }

    public void println(String message) {
        stream.println(message);
    }

}
