package com.ppip.dallyeo.domain.region;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Region ↔ 법정동코드 매핑 (BR-5, business-logic-model §5).
 * 알 수 없는 Region 요청 → 400 BAD_REQUEST.
 */
@Component
public class RegionCodeMapper {

    /**
     * 해당 지역을 조회할 법정동코드 전부. 전주처럼 자치구가 나뉜 지역은 <b>2개 이상</b>이 나오고,
     * 호출자는 코드마다 TourAPI를 부른 뒤 합쳐야 한다.
     */
    public List<LDongCode> toLDongCodes(Region region) {
        List<LDongCode> codes = RegionCatalog.find(region);
        if (codes.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "지원하지 않는 지역입니다: " + region);
        }
        return codes;
    }
}
