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
import javax.net.ssl.TrustManager;
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
                //sslContext = SSLContext.getInstance("SSL");
                /*
                sslContext.init(null, Array(TrustAll), new java.security.SecureRandom())
                HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory)
                HttpsURLConnection.setDefaultHostnameVerifier(VerifiesAllHostNames)
                */
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

/*
import javax.net.ssl._
        import java.security.cert.X509Certificate
        import scala.io.Source

// Bypasses both client and server validation.
        object TrustAll extends X509TrustManager {
        val getAcceptedIssuers = null

        def checkClientTrusted(x509Certificates: Array[X509Certificate], s: String) = {}

        def checkServerTrusted(x509Certificates: Array[X509Certificate], s: String) = {}
        }

// Verifies all host names by simply returning true.
        object VerifiesAllHostNames extends HostnameVerifier {
        def verify(s: String, sslSession: SSLSession) = true
        }

// Main class
        object Test extends App {
        // SSL Context initialization and configuration
        val sslContext = SSLContext.getInstance("SSL")
        sslContext.init(null, Array(TrustAll), new java.security.SecureRandom())
        HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.getSocketFactory)
        HttpsURLConnection.setDefaultHostnameVerifier(VerifiesAllHostNames)

        // Actual call
        val html = Source.fromURL("https://scans.io/json")
        println(html.mkString)
        }
        */