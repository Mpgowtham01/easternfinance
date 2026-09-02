package com.nse.utils;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.http.io.SocketConfig;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.apache.hc.core5.util.TimeValue;
import org.apache.hc.core5.util.Timeout;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;

public class RestTemplateFactory {

    // NSE reports can be slow, but a hung upstream must not hold a Tomcat thread forever.
    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(50);
    private static final Timeout READ_TIMEOUT = Timeout.ofSeconds(120);
    private static final Timeout CONNECTION_REQUEST_TIMEOUT = Timeout.ofSeconds(30);

    // Shared instance: a new pool per call meant a fresh TLS handshake every request and leaked sockets.
    private static volatile RestTemplate restTemplate;

    public static RestTemplate createRestTemplate() throws Exception {
        RestTemplate local = restTemplate;
        if (local == null) {
            synchronized (RestTemplateFactory.class) {
                local = restTemplate;
                if (local == null) {
                    local = build();
                    restTemplate = local;
                }
            }
        }
        return local;
    }

    private static RestTemplate build() throws Exception {
        // Create SSL context that trusts all certs
        SSLContext sslContext = SSLContextBuilder.create()
                .loadTrustMaterial(null, (certificate, authType) -> true)
                .build();

        // Create socket factory
        SSLConnectionSocketFactory socketFactory = new SSLConnectionSocketFactory(
                sslContext,
                new String[]{"TLSv1.3"}, // supported protocols
                null,
                NoopHostnameVerifier.INSTANCE
        );

        ConnectionConfig connectionConfig = ConnectionConfig.custom()
                .setConnectTimeout(CONNECT_TIMEOUT)
                .setSocketTimeout(READ_TIMEOUT)
                .setValidateAfterInactivity(TimeValue.ofSeconds(10))
                .build();

        SocketConfig socketConfig = SocketConfig.custom()
                .setSoTimeout(READ_TIMEOUT)
                .build();

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECTION_REQUEST_TIMEOUT)
                .setResponseTimeout(READ_TIMEOUT)
                .build();

        // Build httpclient5
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(socketFactory)
                                .setDefaultConnectionConfig(connectionConfig)
                                .setDefaultSocketConfig(socketConfig)
                                .setMaxConnTotal(100)
                                .setMaxConnPerRoute(50)
                                .build()
                )
                .setDefaultRequestConfig(requestConfig)
                .evictIdleConnections(TimeValue.ofSeconds(30))
                .disableRedirectHandling()
                // Without this a read timeout is retried once, so a slow NSE report blocked
                // the caller for 2 x READ_TIMEOUT. These POSTs are reports, not idempotent
                // writes worth replaying, so one attempt is enough.
                .disableAutomaticRetries()
                .build();

        // NOTE: HttpComponentsClientHttpRequestFactory has no setReadTimeout in Spring 6.1
        // (that is why the old call was commented out). The read timeout must be configured
        // on the Apache client above, via SocketConfig / ConnectionConfig / RequestConfig.
        return new RestTemplate(new HttpComponentsClientHttpRequestFactory(httpClient));
    }
}
