package com.ppip.dallyeo.place;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.domain.category.CategoryType;
import com.ppip.dallyeo.domain.region.Region;
import com.ppip.dallyeo.place.dto.PlaceDetail;
import com.ppip.dallyeo.place.dto.PlaceSummary;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 장소 조회 API (US-PLACE-1~4). 공개(permitAll). 잘못된 값 → 400 (BR-U3-4).
 */
@RestController
@RequestMapping("/places")
public class PlaceController {

    private static final int DEFAULT_RADIUS = 1000;   // P4

    private final PlaceService placeService;
    private final PlaceDetailAssembler placeDetailAssembler;

    public PlaceController(PlaceService placeService, PlaceDetailAssembler placeDetailAssembler) {
        this.placeService = placeService;
        this.placeDetailAssembler = placeDetailAssembler;
    }

    /** 키워드 검색 (US-PLACE-1). */
    @GetMapping("/search")
    public ApiResponse<List<PlaceSummary>> search(
            @RequestParam String keyword,
            @RequestParam(required = false) String region,
            @RequestParam(required = false) String category) {
        if (keyword == null || keyword.isBlank()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "keyword는 필수입니다.");
        }
        return ApiResponse.success(placeService.search(keyword, parseRegion(region), parseCategory(category)));
    }

    /** 지역 장소 목록 (US-PLACE-2). region 필수. */
    @GetMapping
    public ApiResponse<List<PlaceSummary>> list(
            @RequestParam String region,
            @RequestParam(required = false) String category) {
        Region r = parseRegion(region);
        if (r == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "region은 필수입니다.");
        }
        return ApiResponse.success(placeService.listByRegion(r, parseCategory(category)));
    }

    /** 반경 주변 (US-PLACE-3). radius 기본 1000m. */
    @GetMapping("/nearby")
    public ApiResponse<List<PlaceSummary>> nearby(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(required = false, defaultValue = "1000") int radius,
            @RequestParam(required = false) String category) {
        if (radius <= 0) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "radius는 0보다 커야 합니다.");
        }
        return ApiResponse.success(placeService.nearby(lat, lng, radius, parseCategory(category)));
    }

    /** 장소 상세 (US-PLACE-4). */
    @GetMapping("/{id}")
    public ApiResponse<PlaceDetail> detail(@PathVariable String id) {
        return ApiResponse.success(placeDetailAssembler.assemble(id));
    }

    private Region parseRegion(String region) {
        if (region == null || region.isBlank()) {
            return null;
        }
        try {
            return Region.valueOf(region.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 지역입니다: " + region);
        }
    }

    private CategoryType parseCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        try {
            return CategoryType.valueOf(category.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 카테고리입니다: " + category);
        }
    }
}
