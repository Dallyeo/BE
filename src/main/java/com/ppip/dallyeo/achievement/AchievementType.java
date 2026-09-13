package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.domain.region.Region;

/**
 * 업적 카탈로그(21종). 시안 기준 코드·이름·설명·정렬은 프론트 전달본
 * ({@code resources/data/achievements.json}, README)과 1:1로 맞춘다.
 *
 * <p>조건 로직이 데이터에 결합돼 있어 enum으로 관리한다. 판정 종류는 {@link Kind} 참고.
 * <b>{@link Kind#PENDING}은 판정 기준이 아직 안 정해진 업적</b>이다 — 목록에는 나오지만
 * 자동으로 달성되지 않는다(기준이 정해지면 Kind만 바꾸면 된다).
 *
 * <p>도장 이미지는 파일명이 코드에서 결정된다: {@code /images/achievements/{code 소문자}_on.webp}
 * (미획득은 {@code _off}). 그래서 이미지 경로를 따로 들고 있지 않는다.
 */
public enum AchievementType {

    // ===== 군산 =====
    GUNSAN_SEONYUDO("선유도 짱", "군산의 선유도 해변 코스를 완주했다.",
            Category.GUNSAN, 10, Kind.COURSE, null, "gunsan-seonyudo-beach-run"),
    GUNSAN_CONQUEROR("군산 런트립 정복자", "군산의 모든 추천코스를 완주했다.",
            Category.GUNSAN, 20, Kind.REGION_ALL, Region.GUNSAN, null),
    JJAMPPONG("짬뽕을 먹을 자격이 있는 자", "군산의 짬뽕거리 코스를 완주했다.",
            Category.GUNSAN, 30, Kind.COURSE, null, "gunsan-jjamppong-run"),
    GUNSAN_BEGINNER("군산 초보 러너", "군산의 러닝코스를 즐겨봤다.",
            Category.GUNSAN, 40, Kind.REGION_ANY, Region.GUNSAN, null),
    NATURE_LOVER("자연을 사랑해!", "군산의 편백나무 숲 코스를 완주했다.",
            Category.GUNSAN, 50, Kind.COURSE, null, "gunsan-cypress-forest-run"),
    BETWEEN_WAVES("부숴지는 파도들 사이에서", "군산의 새만금 방파제 코스를 완주했다.",
            Category.GUNSAN, 60, Kind.COURSE, null, "gunsan-saemangeum-run"),

    // ===== 전주 =====
    // 방문형 3종은 해당 코스가 아직 없어 판정 보류(PENDING). 코스가 생기면 Kind.COURSE + courseId로 교체.
    JEONJU_CHERRY_BLOSSOM("천변벚꽃", "삼천변 벚꽃길 방문",
            Category.JEONJU, 70, Kind.PENDING),
    JEONJU_BEGINNER("전주 초보 러너", "전주의 러닝코스를 즐겨봤다.",
            Category.JEONJU, 80, Kind.REGION_ANY, Region.JEONJU, null),
    JEONJU_DEOKJIN_LAKE("덕진 호수", "덕진호수의 연꽃들 관람",
            Category.JEONJU, 90, Kind.PENDING),
    JEONJU_CONQUEROR("전주 런트립 정복자", "전주의 모든 추천코스를 완주했다.",
            Category.JEONJU, 100, Kind.REGION_ALL, Region.JEONJU, null),
    JEONJU_PILGRIM("전주 성지순례자", "전주의 천주교 성지 코스를 완주했다.",
            Category.JEONJU, 110, Kind.COURSE, null, "jeonju-catholic-shrine-run"),
    JEONJU_ECO_MUSEUM("전주 자연생태관", "전주 자연생태관 방문",
            Category.JEONJU, 120, Kind.PENDING),

    // ===== 공통 =====
    LONG_RUN_3H("장기간 러닝 성공", "3시간 넘게 런트립",
            Category.COMMON, 130, Kind.SINGLE_DURATION_SECONDS, 3 * 60 * 60),
    FINISH_10("완주 10회 달성", "완주 10회",
            Category.COMMON, 140, Kind.RUN_COUNT, 10),
    ICE_CREAM_RUNNER("아이스크림 러너", "12월 중 런트립 완주",
            Category.COMMON, 150, Kind.FINISH_IN_MONTH, 12),
    DISTANCE_100KM("100km 이상", "누적거리 달성",
            Category.COMMON, 160, Kind.TOTAL_DISTANCE_METERS, 100_000),
    EARLY_BIRD("얼리버드", "아침 8시 이전에 런트립 시작",
            Category.COMMON, 170, Kind.START_BEFORE_HOUR, 8),
    WAYPOINT_3("경유지 3개 지나감", "경유지 3개 거치고 완주",
            Category.COMMON, 180, Kind.COURSE_WAYPOINTS, 3),
    // 도착지 장소 정보를 앱이 보내주지 않아 판정 불가 — 전달 형식이 정해지면 Kind 교체.
    REST_TIME("휴식타임", "카페, 음식점을 도착지로 설정하고 러닝 완주",
            Category.COMMON, 190, Kind.PENDING),
    // 기준 페이스(키로당 몇 분)가 시안에 없어 보류 — 정해지면 Kind.MIN_PACE_SECONDS + 임계값.
    SLOW_WALKER("뚜벅이", "완주시 페이스가 키로당 몇분",
            Category.COMMON, 200, Kind.PENDING),
    PIONEER("개척자", "새로운 코스를 만들어 런트립",
            Category.COMMON, 210, Kind.FREE_RUN);

    /** 화면 상단 지역 탭 분기용. COMMON은 지역 무관. */
    public enum Category {
        GUNSAN, JEONJU, COMMON
    }

    /** 판정 종류. */
    public enum Kind {
        /** 특정 코스 완주(courseId). */
        COURSE,
        /** 해당 지역 코스 중 하나 이상 완주. */
        REGION_ANY,
        /** 해당 지역 모든 코스 완주. */
        REGION_ALL,
        /** 단일 러닝의 소요 시간이 임계값(초) 초과. */
        SINGLE_DURATION_SECONDS,
        /** 누적 러닝 횟수가 임계값 이상. */
        RUN_COUNT,
        /** 누적 러닝 거리(m)가 임계값 이상. */
        TOTAL_DISTANCE_METERS,
        /** 종료 시각(한국시간)의 월이 임계값과 일치. */
        FINISH_IN_MONTH,
        /** 시작 시각(한국시간)이 임계값 시(時) 이전 — 자정 이후부터. */
        START_BEFORE_HOUR,
        /** 완주한 코스 중 경유지가 임계값 개 이상인 것이 있음. */
        COURSE_WAYPOINTS,
        /** 공식 코스가 아닌(courseId 없는) 러닝 완주. */
        FREE_RUN,
        /** 판정 기준 미확정 — 목록에는 나오되 자동 달성하지 않는다. */
        PENDING
    }

    private final String displayName;
    private final String description;
    private final Category category;
    private final int sortOrder;
    private final Kind kind;
    private final Region region;
    private final String courseId;
    private final int threshold;

    /** 코스·지역 기반(기존 8종). */
    AchievementType(String displayName, String description, Category category, int sortOrder,
                    Kind kind, Region region, String courseId) {
        this(displayName, description, category, sortOrder, kind, region, courseId, 0);
    }

    /** 임계값 기반(횟수·거리·시간 등). */
    AchievementType(String displayName, String description, Category category, int sortOrder,
                    Kind kind, int threshold) {
        this(displayName, description, category, sortOrder, kind, null, null, threshold);
    }

    /** 판정 기준이 없는 종류(PENDING, FREE_RUN처럼 임계값이 필요 없는 것). */
    AchievementType(String displayName, String description, Category category, int sortOrder, Kind kind) {
        this(displayName, description, category, sortOrder, kind, null, null, 0);
    }

    AchievementType(String displayName, String description, Category category, int sortOrder,
                    Kind kind, Region region, String courseId, int threshold) {
        this.displayName = displayName;
        this.description = description;
        this.category = category;
        this.sortOrder = sortOrder;
        this.kind = kind;
        this.region = region;
        this.courseId = courseId;
        this.threshold = threshold;
    }

    public String displayName() {
        return displayName;
    }

    public String description() {
        return description;
    }

    public Category category() {
        return category;
    }

    public int sortOrder() {
        return sortOrder;
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

    public int threshold() {
        return threshold;
    }

    /** 획득(컬러) 도장 이미지 경로. 파일명은 코드에서 결정된다. */
    public String iconOnUrl() {
        return "/images/achievements/" + name().toLowerCase() + "_on.webp";
    }

    /** 미획득(흑백) 도장 이미지 경로. */
    public String iconOffUrl() {
        return "/images/achievements/" + name().toLowerCase() + "_off.webp";
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
