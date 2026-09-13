package com.ppip.dallyeo.domain.region;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegionCodeMapperTest {

    private final RegionCodeMapper mapper = new RegionCodeMapper();

    @Test
    void gunsanIsOneDistrict() {
        assertThat(mapper.toLDongCodes(Region.GUNSAN))
                .containsExactly(new LDongCode("52", "130"));
    }

    @Test
    void jeonjuSplitsIntoItsTwoDistricts() {
        // 전주시(110)는 TourAPI 등록 데이터가 0건이라 쓰면 안 된다 — 실제 데이터는 완산구/덕진구에 있다.
        assertThat(mapper.toLDongCodes(Region.JEONJU))
                .containsExactly(new LDongCode("52", "111"), new LDongCode("52", "113"));
        assertThat(mapper.toLDongCodes(Region.JEONJU))
                .extracting(LDongCode::lDongSignguCd).doesNotContain("110");
    }

    @Test
    void nullRegionIsBadRequest() {
        assertThatThrownBy(() -> mapper.toLDongCodes(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
