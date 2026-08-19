package com.user.utils;

import org.apache.hc.client5.http.classic.HttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.client5.http.ssl.NoopHostnameVerifier;
import org.apache.hc.client5.http.ssl.SSLConnectionSocketFactory;
import org.apache.hc.core5.ssl.SSLContextBuilder;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import javax.net.ssl.SSLContext;
import java.time.Duration;

public class RestTemplateFactory {

    public static RestTemplate createRestTemplate() throws Exception {
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

        // Build httpclient5
        HttpClient httpClient = HttpClients.custom()
                .setConnectionManager(
                        PoolingHttpClientConnectionManagerBuilder.create()
                                .setSSLSocketFactory(socketFactory)
                                .build()
                )
                .disableRedirectHandling()
                .build();

        // Create request factory and set timeouts
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);
        requestFactory.setConnectTimeout(Duration.ofSeconds(50));
//        requestFactory.setReadTimeout(Duration.ofSeconds(100));

        return new RestTemplate(requestFactory);
    }
}
