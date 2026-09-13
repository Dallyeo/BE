package com.ppip.dallyeo.external.tourapi;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.ppip.dallyeo.domain.region.LDongCode;
import com.ppip.dallyeo.external.tourapi.dto.TourItem;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import java.time.Duration;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * TourApiClient 파싱/정규화 검증 (WireMock). Spring 프록시 없이 직접 생성하므로
 * 캐시/Resilience4j/폴백(AOP)은 이 테스트 범위 밖 — Build & Test(통합)에서 검증한다.
 */
class TourApiClientTest {

    private WireMockServer wm;
    private TourApiClient client;

    @BeforeEach
    void setUp() {
        wm = new WireMockServer(options().dynamicPort());
        wm.start();

        RestClient restClient = RestClient.builder().baseUrl(wm.baseUrl()).build();
        TourApiProperties props = new TourApiProperties(
                wm.baseUrl(), "TESTKEY",
                Duration.ofSeconds(2), Duration.ofSeconds(3), Duration.ofMinutes(30),
                Duration.ofHours(24), 4, Duration.ofSeconds(4));
        client = new TourApiClient(restClient, props, new TourApiNormalizer());
    }

    @AfterEach
    void tearDown() {
        wm.stop();
    }

    @Test
    void parsesAndNormalizesAreaBasedList() {
        String body = """
                {"response":{"header":{"resultCode":"0000"},"body":{"items":{"item":[
                  {"contentid":"101","title":"군산 장소","addr1":"전북 군산시","mapx":"126.7","mapy":"35.98","firstimage2":"","contenttypeid":"12"},
                  {"contentid":"102","title":"카페","addr1":"전북 군산시","mapx":"","mapy":"","contenttypeid":"39"}
                ]},"numOfRows":10,"pageNo":1,"totalCount":2}}}
                """;
        wm.stubFor(get(urlPathEqualTo("/areaBasedList2"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        List<TourItem> items = client.areaBasedList(new LDongCode("52", "130"), null, 1, 10);

        assertThat(items).hasSize(2);
        assertThat(items.get(0).contentId()).isEqualTo("101");
        assertThat(items.get(0).longitude()).isEqualTo(126.7);
        assertThat(items.get(0).thumbnailUrl()).isNull();   // "" → null
        assertThat(items.get(1).longitude()).isNull();      // area: 좌표 null 유지, 보존
    }

    @Test
    void emptyResultReturnsEmptyList() {
        String body = """
                {"response":{"header":{"resultCode":"0000"},"body":{"items":"","numOfRows":10,"pageNo":1,"totalCount":0}}}
                """;
        wm.stubFor(get(urlPathEqualTo("/areaBasedList2"))
                .willReturn(aResponse().withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        List<TourItem> items = client.areaBasedList(new LDongCode("52", "110"), 12, 1, 10);
        assertThat(items).isEmpty();
    }

    @Test
    void serverErrorPropagates() {
        // AOP 미적용(직접 생성)이라 폴백/EXTERNAL_API_ERROR 변환은 일어나지 않고 원 예외가 전파된다.
        // 폴백 정규화는 Build & Test(통합, Spring context)에서 검증.
        wm.stubFor(get(urlPathEqualTo("/areaBasedList2"))
                .willReturn(aResponse().withStatus(500)));

        assertThatThrownBy(() -> client.areaBasedList(new LDongCode("52", "130"), null, 1, 10))
                .isInstanceOf(RuntimeException.class);
    }
}
