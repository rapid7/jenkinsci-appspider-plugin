/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import org.apache.http.HttpResponse;

import java.util.List;
import java.util.Objects;

/**
 * various utility methods that have no better home
 */
public class FunctionalUtility {

    private FunctionalUtility() {
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


    /**
     * determines if the response was successful based on status code
     * @param response HttpResponse object to check
     * @return true if status code is in the 200 range; otherwise, false
     */
    public static boolean isSuccessStatusCode(HttpResponse response) {
        if (Objects.isNull(response))
            return false;

        // https://www.w3.org/Protocols/HTTP/HTRESP.html
        // while most if not all AppSpider Enterprise endpoints return 200 on success it's
        // safer to treat any success code as success
        int statusCode = response.getStatusLine().getStatusCode();
        return statusCode >= 200 && statusCode <= 299;
    }
}
