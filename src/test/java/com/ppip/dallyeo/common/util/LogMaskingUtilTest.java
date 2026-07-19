package com.ppip.dallyeo.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class LogMaskingUtilTest {

    @Test
    void masksServiceKeyValue() {
        String url = "https://api/areaBasedList2?serviceKey=SECRET123&pageNo=1";
        assertThat(LogMaskingUtil.maskServiceKey(url))
                .isEqualTo("https://api/areaBasedList2?serviceKey=***&pageNo=1");
    }

    @Test
    void masksServiceKeyAtEnd() {
        assertThat(LogMaskingUtil.maskServiceKey("serviceKey=abcDEF=="))
                .isEqualTo("serviceKey=***");
    }

    @Test
    void leavesOtherParamsUntouched() {
        assertThat(LogMaskingUtil.maskServiceKey("pageNo=1&numOfRows=10"))
                .isEqualTo("pageNo=1&numOfRows=10");
    }

    @Test
    void handlesNull() {
        assertThat(LogMaskingUtil.maskServiceKey(null)).isNull();
    }
}
