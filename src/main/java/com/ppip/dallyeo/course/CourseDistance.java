package com.ppip.dallyeo.course;

/**
 * 코스 거리 분류 (BR-U2-1). API/저장 값은 SHORT/MEDIUM/LONG.
 * courses.json의 한글(단거리/중거리/장거리)은 {@link #fromKorean}으로 변환.
 */
public enum CourseDistance {
    SHORT,
    MEDIUM,
    LONG;

    /** 한글 분류 → enum. 미지원 문자열이면 null(적재 스킵 판단용). */
    public static CourseDistance fromKorean(String korean) {
        if (korean == null) {
            return null;
        }
        return switch (korean.trim()) {
            case "단거리" -> SHORT;
            case "중거리" -> MEDIUM;
            case "장거리" -> LONG;
            default -> null;
        };
    }

    /** API 파라미터 문자열(SHORT/MEDIUM/LONG) → enum. 잘못된 값이면 null(400 판단용, BR-U2-4). */
    public static CourseDistance fromParam(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return CourseDistance.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
