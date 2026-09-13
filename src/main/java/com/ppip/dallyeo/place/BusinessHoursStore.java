package com.ppip.dallyeo.place;

import com.ppip.dallyeo.external.tourapi.TourApiProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 영업시간 보관소 — TourAPI 호출 전에 여기부터 본다.
 *
 * <p>{@link PlaceBusinessHours} 를 감싸 "충분히 최신인가"를 판단한다.
 * 신선도 기준은 {@code tourapi.hours-refresh-after}.
 */
@Component
public class BusinessHoursStore {

    private static final Logger log = LoggerFactory.getLogger(BusinessHoursStore.class);

    private final PlaceBusinessHoursRepository repository;
    private final TourApiProperties props;

    public BusinessHoursStore(PlaceBusinessHoursRepository repository, TourApiProperties props) {
        this.repository = repository;
        this.props = props;
    }

    /**
     * 보관 중이면서 아직 신선한 항목만 돌려준다(조회 1회).
     * 값이 없는 장소도 보관 대상이라 <b>키는 있고 값이 null</b> 일 수 있다 — 그 경우 재조회하지 않는다.
     */
    @Transactional(readOnly = true)
    public Map<String, Optional<String>> findFresh(Collection<String> contentIds) {
        if (contentIds.isEmpty()) {
            return Map.of();
        }
        Instant threshold = Instant.now().minus(props.hoursRefreshAfter());
        Map<String, Optional<String>> result = new HashMap<>();
        for (PlaceBusinessHours row : repository.findByContentIdIn(contentIds)) {
            if (row.getFetchedAt().isAfter(threshold)) {
                result.put(row.getContentId(), Optional.ofNullable(row.getBusinessHours()));
            }
        }
        return result;
    }

    /** 새로 받은 값을 보관(덮어쓰기). 영업시간이 없는 장소도 그 사실을 남긴다. */
    @Transactional
    public void saveAll(List<PlaceBusinessHours> rows) {
        if (rows.isEmpty()) {
            return;
        }
        repository.saveAll(rows);
        log.debug("영업시간 {}건 보관", rows.size());
    }

    public static PlaceBusinessHours row(String contentId, int contentTypeId, String hours) {
        return PlaceBusinessHours.builder()
                .contentId(contentId)
                .contentTypeId(contentTypeId)
                .businessHours(hours)
                .fetchedAt(Instant.now())
                .build();
    }
}
