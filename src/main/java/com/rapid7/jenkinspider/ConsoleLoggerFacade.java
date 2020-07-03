package com.rapid7.jenkinspider;

import com.rapid7.appspider.LoggerFacade;

class ConsoleLoggerFacade implements LoggerFacade {

    @Override
    public void println(String message) {
        System.out.println(message);
    }
}
