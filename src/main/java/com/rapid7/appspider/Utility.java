/*
 * Copyright © 2003 - 2019 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import java.util.List;
import java.util.Objects;

/**
 * various utility merthods that have no better home
 */
class Utility {

    /**
     * Utility is not intended to be created but instead provides static utility methods
     */
    private Utility() {
    }

    /**
     * converts ArrayList{String} to String[]
     * @param source list to convert to array
     * @return String[] containing all members from source
     * @throws IllegalArgumentException if source is null
     */
    public static String[] toStringArray(List<String> source) {
        if (Objects.isNull(source))
            throw new IllegalArgumentException("source cannot be null");
        String[] engines = new String[source.size()];
        return source.toArray(engines);
    }


}
