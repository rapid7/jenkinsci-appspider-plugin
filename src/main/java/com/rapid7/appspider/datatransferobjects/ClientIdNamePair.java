/*
 * Copyright © 2003 - 2021 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider.datatransferobjects;

public final class ClientIdNamePair {

    private final String id;
    private final String name;

    public ClientIdNamePair(String id, String name) {
        this.id = id;
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String getId() {
        return id;
    }

}
