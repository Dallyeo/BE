package com.ppip.dallyeo.config;

import com.ppip.dallyeo.external.tourapi.TourApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * 외부 호출용 RestClient (nfr-design: RestClient + HTTPS + 타임아웃).
 * TourAPI(U1-a) 및 소셜(Kakao/Apple, U4). 소셜은 절대 URI를 사용하므로 baseUrl 없이 구성.
 */
@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
public class RestClientConfig {

    // 소셜 외부호출 타임아웃 — TourAPI(2s/3s)와 동일 기조.
    private static final Duration OAUTH_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final Duration OAUTH_READ_TIMEOUT = Duration.ofSeconds(3);

    @Bean
    public RestClient tourApiRestClient(TourApiProperties props) {
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory(props.connectTimeout(), props.readTimeout()))
                .build();
    }

    /** Kakao 사용자 조회용 (절대 URI 사용). */
    @Bean
    public RestClient kakaoRestClient() {
        return RestClient.builder()
                .requestFactory(factory(OAUTH_CONNECT_TIMEOUT, OAUTH_READ_TIMEOUT))
                .build();
    }

    /** Apple JWKS 조회용 (절대 URI 사용). */
    @Bean
    public RestClient appleRestClient() {
        return RestClient.builder()
                .requestFactory(factory(OAUTH_CONNECT_TIMEOUT, OAUTH_READ_TIMEOUT))
                .build();
    }

    private SimpleClientHttpRequestFactory factory(Duration connectTimeout, Duration readTimeout) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) connectTimeout.toMillis());
        factory.setReadTimeout((int) readTimeout.toMillis());
        return factory;
    }
}
