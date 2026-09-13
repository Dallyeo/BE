package com.ppip.dallyeo.place;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * 장소별 영업시간 영구 보관 (TourAPI detailIntro2 결과).
 *
 * <p>영업시간은 TourAPI에서 <b>장소당 1회 호출</b>로만 얻을 수 있고, 공공데이터포털은
 * 오퍼레이션별 일일 호출 한도가 있다. Redis 캐시(TTL)만 쓰면 만료될 때마다 전 장소를
 * 다시 받아야 해서 한도를 매일 소진한다 — 정작 영업시간은 몇 달에 한 번 바뀔까 말까 한 값인데도.
 *
 * <p>그래서 한 번 받은 값은 여기 남긴다. 재조회는 {@code tourapi.hours-refresh-after} 가 지난 것만.
 * 서버를 재시작해도 남으므로 초기 적재 이후에는 신규 장소분만 호출이 나간다.
 *
 * <p><b>영업시간이 없는 장소도 행을 남긴다</b>({@code businessHours == null}). 그래야
 * "정보 없음"을 기억해 매번 다시 묻지 않는다.
 */
@Entity
@Table(name = "place_business_hours")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlaceBusinessHours {

    /** TourAPI contentId. */
    @Id
    @Column(length = 40)
    private String contentId;

    @Column(nullable = false)
    private int contentTypeId;

    /** 개행(\n) 구분 정리본. 원천에 정보가 없으면 null. */
    @Column(columnDefinition = "TEXT")
    private String businessHours;

    @Column(nullable = false)
    private Instant fetchedAt;
}
