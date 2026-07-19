package com.ppip.dallyeo.domain.region;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RegionCodeMapperTest {

    private final RegionCodeMapper mapper = new RegionCodeMapper();

    @Test
    void mapsGunsan() {
        LDongCode code = mapper.toLDongCode(Region.GUNSAN);
        assertThat(code.lDongRegnCd()).isEqualTo("52");
        assertThat(code.lDongSignguCd()).isEqualTo("130");
    }

    @Test
    void mapsJeonju() {
        LDongCode code = mapper.toLDongCode(Region.JEONJU);
        assertThat(code.lDongRegnCd()).isEqualTo("52");
        assertThat(code.lDongSignguCd()).isEqualTo("110");
    }

    @Test
    void nullRegionIsBadRequest() {
        assertThatThrownBy(() -> mapper.toLDongCode(null))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getErrorCode())
                        .isEqualTo(ErrorCode.BAD_REQUEST));
    }
}
