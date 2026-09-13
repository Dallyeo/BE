package com.ppip.dallyeo.common.util;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 사례는 전부 군산 실데이터(TourAPI detailIntro2)에서 가져왔다.
 * 원문 구분자가 제각각이라 "무엇으로 자르는가"가 이 클래스의 전부다.
 */
class BusinessHoursTextTest {

    @Test
    void normalize_bulletGluedToPreviousItem() {
        // 등대로(1305903) — 구분자가 "- " 뿐이라 그대로 두면 카드 한 줄에서 잘린다.
        assertThat(BusinessHoursText.normalize("- 12:00~21:00- 준비시간 14:00~17:00- 마지막 주문 20:30"))
                .isEqualTo("12:00~21:00\n준비시간 14:00~17:00\n마지막 주문 20:30");
    }

    @Test
    void normalize_bulletWithSurroundingSpaces() {
        // 거목아리랑
        assertThat(BusinessHoursText.normalize("- 11:00~21:30 - 준비시간 14:00~16:30 - 마지막 주문 21:00"))
                .isEqualTo("11:00~21:30\n준비시간 14:00~16:30\n마지막 주문 21:00");
    }

    @Test
    void normalize_brTags() {
        // 국제반점
        assertThat(BusinessHoursText.normalize("- 11:00~19:30<br>- 준비시간 14:30~17:00<br />- 마지막 주문 19:00"))
                .isEqualTo("11:00~19:30\n준비시간 14:30~17:00\n마지막 주문 19:00");
    }

    @Test
    void normalize_parenthesisNote() {
        // 경원해물찜
        assertThat(BusinessHoursText.normalize("12:00~21:00 (준비시간 14:00~17:00)"))
                .isEqualTo("12:00~21:00\n(준비시간 14:00~17:00)");
    }

    @Test
    void normalize_markNoteGlued() {
        // 뚱보식당
        assertThat(BusinessHoursText.normalize("08:00~16:00※ 재료 소진 시 영업 종료"))
                .isEqualTo("08:00~16:00\n※ 재료 소진 시 영업 종료");
    }

    @Test
    void normalize_keepsHyphenTimeRange() {
        // 도란 — 하이픈이 구분자가 아니라 시간 범위. 여기서 자르면 영업시간이 망가진다.
        assertThat(BusinessHoursText.normalize("월, 수~토 11:00-23:00 일 11:00-22:00"))
                .isEqualTo("월, 수~토 11:00-23:00 일 11:00-22:00");
        assertThat(BusinessHoursText.normalize("09:00-18:00")).isEqualTo("09:00-18:00");
    }

    @Test
    void normalize_stripsTagsAndEntities() {
        assertThat(BusinessHoursText.normalize("<p>09:00&nbsp;~&nbsp;18:00</p>")).isEqualTo("09:00 ~ 18:00");
    }

    @Test
    void normalize_blankOrTagOnly_returnsNull() {
        assertThat(BusinessHoursText.normalize("   ")).isNull();
        assertThat(BusinessHoursText.normalize("<br><br>")).isNull();
        assertThat(BusinessHoursText.normalize(null)).isNull();
    }

    @Test
    void first_takesFirstLineWithTime() {
        assertThat(BusinessHoursText.first("12:00~21:00\n준비시간 14:00~17:00")).isEqualTo("12:00~21:00");
    }

    @Test
    void first_skipsHeadingLineWithoutTime() {
        // 남도밥상 — 첫 줄이 "[평일]" 머리글이라 그대로 쓰면 카드에 시간이 안 나온다.
        String hours = BusinessHoursText.normalize(
                "[평일]<br>- 09:00~19:30<br>- 마지막 주문 18:20<br>[주말]<br>- 08:30~20:00");
        assertThat(hours).startsWith("[평일]\n09:00~19:30");
        assertThat(BusinessHoursText.first(hours)).isEqualTo("09:00~19:30");
    }

    @Test
    void first_noTimeAtAll_usesFirstLine() {
        assertThat(BusinessHoursText.first("상시 개방")).isEqualTo("상시 개방");
        assertThat(BusinessHoursText.first(null)).isNull();
    }
}
