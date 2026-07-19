package com.ppip.dallyeo.domain.region;

import com.ppip.dallyeo.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 지역 조회 API (US-REGION-1). 공개(permitAll).
 */
@RestController
@RequestMapping("/regions")
public class RegionController {

    private final RegionService regionService;

    public RegionController(RegionService regionService) {
        this.regionService = regionService;
    }

    @GetMapping
    public ApiResponse<List<RegionResponse>> list() {
        return ApiResponse.success(regionService.getSupportedRegions());
    }
}
