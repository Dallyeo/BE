package com.ppip.dallyeo.place;

import com.ppip.dallyeo.badge.BadgeService;
import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.external.tourapi.TourApiClient;
import com.ppip.dallyeo.external.tourapi.dto.TourCommon;
import com.ppip.dallyeo.external.tourapi.dto.TourIntro;
import com.ppip.dallyeo.place.dto.PlaceDetail;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 장소 상세 조합 (US-PLACE-4, BR-U3-5, P3/D2).
 * detailCommon2 → (contentTypeId 확보) → detailIntro2 순차 → PlaceMapper → BadgeService 부착.
 * common 없음 → 404.
 */
@Service
public class PlaceDetailAssembler {

    private final TourApiClient tourApiClient;
    private final PlaceMapper placeMapper;
    private final BadgeService badgeService;

    public PlaceDetailAssembler(TourApiClient tourApiClient, PlaceMapper placeMapper, BadgeService badgeService) {
        this.tourApiClient = tourApiClient;
        this.placeMapper = placeMapper;
        this.badgeService = badgeService;
    }

    public PlaceDetail assemble(String id) {
        TourCommon common = tourApiClient.detailCommon(id);
        if (common == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "장소를 찾을 수 없습니다: " + id);
        }
        // P3: intro는 common의 contentTypeId 확보 후 순차 호출
        TourIntro intro = tourApiClient.detailIntro(id, common.contentTypeId());
        List<String> badges = badgeService.badgesFor(common.title(), common.address());
        return placeMapper.toDetail(common, intro, common.contentTypeId(), badges);
    }
}
