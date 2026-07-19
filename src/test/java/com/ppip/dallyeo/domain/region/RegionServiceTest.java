package com.ppip.dallyeo.domain.region;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RegionServiceTest {

    private final RegionService service = new RegionService();

    @Test
    void returnsSupportedRegionsWithDisplayNames() {
        List<RegionResponse> regions = service.getSupportedRegions();

        assertThat(regions).containsExactly(
                new RegionResponse("GUNSAN", "군산"),
                new RegionResponse("JEONJU", "전주"));
    }

    @Test
    void catalogRoundTripDisplayName() {
        assertThat(RegionCatalog.displayName(Region.GUNSAN)).isEqualTo("군산");
        assertThat(RegionCatalog.fromDisplayName("전주")).isEqualTo(Region.JEONJU);
        assertThat(RegionCatalog.fromDisplayName("서울")).isNull();
    }
}
