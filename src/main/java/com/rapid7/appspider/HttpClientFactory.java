/*
 * Copyright © 2003 - 2020 Rapid7, Inc.  All rights reserved.
 */

package com.rapid7.appspider;

import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.ssl.SSLContexts;

import javax.net.ssl.SSLContext;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

/**
 * HttpClients configured to use TLS 1.2
 */
public class HttpClientFactory {

    private final SSLConnectionSocketFactory socketFactory;
    final SSLContext sslContext;

    public HttpClientFactory(boolean allowSelfSignedCertificates) {
        try {
            // ignore self-signed certs since we have no control over the server setup and as such can't
            // enforce proper certificate usage
            if (allowSelfSignedCertificates) {
                sslContext = new SSLContextBuilder()
                        .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                        .build();
            } else {
                sslContext = SSLContexts.createDefault();
            }

        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("Unable to configure SSL Context", e);
        }
        socketFactory = new SSLConnectionSocketFactory(sslContext,
                new String[]{"TLSv1.2"},
                null, NoopHostnameVerifier.INSTANCE);
    }

    /**
     * gets a closeble HttpClient configured for TLS 1.2
     * @return closeble HttpClient configured for TLS 1.2
     */
    public CloseableHttpClient getClient() {
        return HttpClients
                .custom()
                .setSSLContext(sslContext)
                .setSSLSocketFactory(socketFactory)
                .build();
    }
}
