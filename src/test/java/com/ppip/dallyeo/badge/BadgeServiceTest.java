package com.ppip.dallyeo.badge;

import com.ppip.dallyeo.domain.region.Region;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BadgeServiceTest {

    private final BadgeRepository repository = mock(BadgeRepository.class);
    private final BadgeService service = new BadgeService(repository, new AddressNormalizer());

    private Badge badge(BadgeType type) {
        return Badge.builder().type(type).region(Region.GUNSAN)
                .placeName("아서원").normalizedName("아서원").normalizedAddress("전북군산시경암5길63").build();
    }

    @Test
    void returnsBadgeTypesForMatch() {
        when(repository.findByNormalizedNameAndNormalizedAddress(anyString(), anyString()))
                .thenReturn(List.of(badge(BadgeType.GOOD_PRICE)));

        List<String> badges = service.badgesFor("아서원", "전북특별자치도 군산시 경암5길 63");

        assertThat(badges).containsExactly("GOOD_PRICE");
    }

    @Test
    void emptyWhenNoMatch() {
        when(repository.findByNormalizedNameAndNormalizedAddress(anyString(), anyString()))
                .thenReturn(List.of());

        assertThat(service.badgesFor("없는집", "어딘가")).isEmpty();
    }

    @Test
    void nullInputsReturnEmpty() {
        assertThat(service.badgesFor(null, "주소")).isEmpty();
        assertThat(service.badgesFor("이름", null)).isEmpty();
    }
}
