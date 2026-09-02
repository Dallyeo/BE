package com.ppip.dallyeo.badge;

import com.ppip.dallyeo.domain.region.Region;
import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
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

    // ===== 목록용 일괄 매칭 (N+1 방지) =====

    private final PlaceKey matching = new PlaceKey("아서원", "전북특별자치도 군산시 경암5길 63");
    private final PlaceKey sameNameOtherAddress = new PlaceKey("아서원", "전북 군산시 다른길 1");
    private final PlaceKey unmatched = new PlaceKey("없는집", "전북 군산시 어딘가 5");

    @Test
    void badgesForAll_matchesOnlyWhenNameAndAddressBothMatch() {
        when(repository.findByNormalizedNameIn(anyCollection()))
                .thenReturn(List.of(badge(BadgeType.GOOD_PRICE)));

        Map<PlaceKey, List<String>> result =
                service.badgesForAll(List.of(matching, sameNameOtherAddress, unmatched));

        assertThat(result.get(matching)).containsExactly("GOOD_PRICE");
        // 업소명은 같지만 주소가 다르면 탈락 — IN 조회로 좁힌 뒤 주소까지 대조하기 때문.
        assertThat(result.get(sameNameOtherAddress)).isEmpty();
        assertThat(result.get(unmatched)).isEmpty();
    }

    @Test
    void badgesForAll_queriesRepositoryOnce() {
        when(repository.findByNormalizedNameIn(anyCollection())).thenReturn(List.of());

        service.badgesForAll(List.of(matching, sameNameOtherAddress, unmatched));

        verify(repository).findByNormalizedNameIn(anyCollection());
        verify(repository, never()).findByNormalizedNameAndNormalizedAddress(anyString(), anyString());
    }

    @Test
    void badgesForAll_returnsEntryForEveryInputKey() {
        when(repository.findByNormalizedNameIn(anyCollection())).thenReturn(List.of());

        Collection<PlaceKey> input = List.of(matching, unmatched, new PlaceKey(null, null));

        assertThat(service.badgesForAll(input)).containsOnlyKeys(input.toArray(new PlaceKey[0]));
    }

    @Test
    void badgesForAll_emptyInputSkipsQuery() {
        assertThat(service.badgesForAll(List.of())).isEmpty();
        verify(repository, never()).findByNormalizedNameIn(anyCollection());
    }
}
