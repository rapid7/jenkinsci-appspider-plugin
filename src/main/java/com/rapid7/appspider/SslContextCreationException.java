package com.rapid7.appspider;

/**
 * Exception thrown when unable to create a SSL context for use in HttpClient
 */
public class SslContextCreationException extends Exception {

    public SslContextCreationException(String message, Throwable cause) {
        super(message, cause);
    }

}

