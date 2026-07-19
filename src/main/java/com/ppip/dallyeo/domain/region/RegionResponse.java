package com.ppip.dallyeo.domain.region;

/**
 * 지역 목록 응답 항목 (US-REGION-1). 예: {"GUNSAN","군산"}.
 */
public record RegionResponse(String code, String name) {
}
