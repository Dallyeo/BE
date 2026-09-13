package com.ppip.dallyeo.badge;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * 실제 classpath CSV(모범음식점 UTF-8 52 / 착한가격 EUC-KR 60→요식업 45)로 적재 로직 검증. DB는 mock.
 * 기동마다 <b>전량 교체</b>(deleteAll → saveAll) 이므로 그 계약을 검증한다.
 */
class BadgeCsvLoaderTest {

    private final BadgeRepository repository = mock(BadgeRepository.class);
    private final BadgeCsvLoader loader = new BadgeCsvLoader(repository, new AddressNormalizer());

    @SuppressWarnings("unchecked")
    private List<Badge> captureSaved() {
        ArgumentCaptor<Iterable<Badge>> captor = ArgumentCaptor.forClass(Iterable.class);
        verify(repository).saveAll(captor.capture());
        List<Badge> saved = new ArrayList<>();
        captor.getValue().forEach(saved::add);
        return saved;
    }

    @Test
    void loadsModelAndFoodOnlyGoodPrice() {
        loader.run(null);

        List<Badge> saved = captureSaved();
        // 모범음식점 52 + 착한가격 요식업 45(비요식 15 제외) = 97
        assertThat(saved).hasSize(97);
        assertThat(saved).filteredOn(b -> b.getType() == BadgeType.MODEL_RESTAURANT).hasSize(52);
        assertThat(saved).filteredOn(b -> b.getType() == BadgeType.GOOD_PRICE).hasSize(45);
    }

    @Test
    void replacesExistingRowsBeforeInserting() {
        // 정규화 규칙이 바뀌어도 옛 키로 만든 행이 남지 않도록 delete → insert 순서를 보장한다.
        loader.run(null);

        InOrder order = inOrder(repository);
        order.verify(repository).deleteAllInBatch();
        order.verify(repository).saveAll(anyIterable());
    }

    @Test
    void rerunProducesSameDataset() {
        loader.run(null);
        loader.run(null);

        verify(repository, times(2)).deleteAllInBatch();
        verify(repository, times(2)).saveAll(anyIterable());
    }

    @Test
    void normalizedKeysAreUnique() {
        loader.run(null);

        List<String> keys = captureSaved().stream()
                .map(b -> b.getType() + "|" + b.getNormalizedName() + "|" + b.getNormalizedAddress())
                .toList();
        assertThat(keys).doesNotHaveDuplicates();
    }

    @Test
    void addressesKeepBuildingNumberAfterDetailAddressCut() {
        // 상세주소 절단이 건물번호까지 깎지 않는지(정규화 회귀 방지) — 숫자 없는 주소가 없어야 한다.
        assertThat(captureSavedAfterRun())
                .allSatisfy(b -> assertThat(b.getNormalizedAddress()).matches(".*\\d.*"));
    }

    private List<Badge> captureSavedAfterRun() {
        loader.run(null);
        return captureSaved();
    }
}
