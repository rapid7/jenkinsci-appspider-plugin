package com.rapid7.appspider;

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

    public HttpClientFactory() {
        try {
            // ignore self-signed certs since we have no control over the server setup and as such can't
            // enforce proper certificate usage
            sslContext = new SSLContextBuilder()
                    .loadTrustMaterial(null, (x509CertChain, authType) -> true)
                    .build();
        } catch (NoSuchAlgorithmException | KeyStoreException | KeyManagementException e) {
            throw new RuntimeException("Unable to configure SSL Context", e);
        }
        socketFactory = new SSLConnectionSocketFactory(SSLContexts.createDefault(),
                new String[] { "TLSv1.2" },
                null, SSLConnectionSocketFactory.getDefaultHostnameVerifier());
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
