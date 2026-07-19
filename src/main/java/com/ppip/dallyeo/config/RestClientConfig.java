package com.ppip.dallyeo.config;

import com.ppip.dallyeo.external.tourapi.TourApiProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

/**
 * 외부 TourAPI 호출용 RestClient (nfr-design: RestClient + HTTPS + 타임아웃).
 * connect/read 타임아웃은 TourApiProperties(N2: 2s/3s)에서 주입.
 */
@Configuration
@EnableConfigurationProperties(TourApiProperties.class)
public class RestClientConfig {

    @Bean
    public RestClient tourApiRestClient(TourApiProperties props) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout((int) props.connectTimeout().toMillis());
        factory.setReadTimeout((int) props.readTimeout().toMillis());
        return RestClient.builder()
                .baseUrl(props.baseUrl())
                .requestFactory(factory)
                .build();
    }
}
