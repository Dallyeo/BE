package com.ppip.dallyeo.domain.region;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

/**
 * Region ↔ 법정동코드 매핑 (BR-5, business-logic-model §5).
 * 알 수 없는 Region 요청 → 400 BAD_REQUEST.
 */
@Component
public class RegionCodeMapper {

    public LDongCode toLDongCode(Region region) {
        LDongCode code = RegionCatalog.find(region);
        if (code == null) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 지역입니다: " + region);
        }
        return code;
    }
}
