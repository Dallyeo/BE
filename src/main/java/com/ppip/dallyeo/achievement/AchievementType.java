package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.domain.region.Region;

/**
 * 업적 카탈로그(고정 8종, img_1.png). 조건 로직이 데이터에 결합돼 있어 enum으로 관리.
 * 판정 기준은 courseId(Q2=A): 코스 완주 = 해당 courseId로 저장된 run 존재,
 * 지역 러닝/정복 = 해당 지역 코스(courseId)들로 판정. 지역 코스 집합은 실데이터(course 테이블)로 해석.
 */
public enum AchievementType {

    GUNSAN_BEGINNER("군산 초보 러너", "군산에서 러닝을 완료한 사람", Kind.REGION_ANY, Region.GUNSAN, null),
    JJAMPPONG("짬뽕을 먹을 자격이 있는 자", "군산 짬뽕거리 코스를 완주한 사람", Kind.COURSE, null, "gunsan-jjamppong-run"),
    GUNSAN_CONQUEROR("군산 런트립 정복자", "군산의 모든 추천 코스를 완주한 사람", Kind.REGION_ALL, Region.GUNSAN, null),
    JEONJU_BEGINNER("전주 초보 러너", "전주에서 러닝을 완료한 사람", Kind.REGION_ANY, Region.JEONJU, null),
    JEONJU_CONQUEROR("전주 런트립 정복자", "전주의 모든 추천 코스를 완주한 사람", Kind.REGION_ALL, Region.JEONJU, null),
    JEONJU_PILGRIM("전주 성지순례자", "전주의 천주교 성지 코스를 완주한 사람", Kind.COURSE, null, "jeonju-catholic-shrine-run"),
    NATURE_LOVER("자연을 사랑해!", "군산의 편백나무 숲 코스를 완주한 사람", Kind.COURSE, null, "gunsan-cypress-forest-run"),
    BETWEEN_WAVES("부숴지는 파도를 사이에서", "군산의 새만금 방파제 코스를 완주한 사람", Kind.COURSE, null, "gunsan-saemangeum-run");

    /** 판정 종류. */
    public enum Kind {
        /** 특정 코스 완주(courseId). */
        COURSE,
        /** 해당 지역 코스 중 하나 이상 완주. */
        REGION_ANY,
        /** 해당 지역 모든 코스 완주. */
        REGION_ALL
    }

    private final String displayName;
    private final String description;
    private final Kind kind;
    private final Region region;
    private final String courseId;

    AchievementType(String displayName, String description, Kind kind, Region region, String courseId) {
        this.displayName = displayName;
        this.description = description;
        this.kind = kind;
        this.region = region;
        this.courseId = courseId;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Kind kind() {
        return kind;
    }

    public Region region() {
        return region;
    }

    public String courseId() {
        return courseId;
    }

    /** 코드(enum 이름) → 타입. 미지원이면 null(404 판단용). */
    public static AchievementType fromCode(String code) {
        if (code == null || code.isBlank()) {
            return null;
        }
        try {
            return AchievementType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
