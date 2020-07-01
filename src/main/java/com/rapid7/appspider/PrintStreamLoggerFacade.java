package com.rapid7.appspider;

import java.io.PrintStream;

class PrintStreamLoggerFacade implements LoggerFacade {

    private final PrintStream stream;

    PrintStreamLoggerFacade(PrintStream stream) {
        this.stream = stream;
    }

    public void println(String message) {
        stream.println(message);
    }

}
