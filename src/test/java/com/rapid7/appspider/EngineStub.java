/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import java.util.UUID;

public class EngineStub {

    private final String id;
    private final String name;

    EngineStub(String id) {
       this.id = id;
       name = "engine" + UUID.randomUUID().toString();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
