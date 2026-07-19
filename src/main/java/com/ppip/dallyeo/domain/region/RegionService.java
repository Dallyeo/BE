package com.ppip.dallyeo.domain.region;

import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 지원 지역 목록 제공 (US-REGION-1). RegionCatalog 정의(코드+표시명) 기반.
 */
@Service
public class RegionService {

    public List<RegionResponse> getSupportedRegions() {
        return RegionCatalog.supportedRegions().stream()
                .map(r -> new RegionResponse(r.name(), RegionCatalog.displayName(r)))
                .toList();
    }
}
