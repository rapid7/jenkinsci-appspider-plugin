package com.rapid7;

import com.rapid7.appspider.LoggerFacade;

public class ConsoleLoggerFacade implements LoggerFacade {

    @Override
    public void println(String message) {
        System.out.println(message);
    }
}
