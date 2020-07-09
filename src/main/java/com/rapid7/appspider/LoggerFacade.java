/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

public interface LoggerFacade {
    void println(String message);
    void info(String message);
    void warn(String message);
    void severe(String message);
}
