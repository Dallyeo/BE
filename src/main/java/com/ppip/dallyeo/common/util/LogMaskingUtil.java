package com.ppip.dallyeo.common.util;

import java.util.regex.Pattern;

/**
 * 민감정보 로그 마스킹 (D5, 보안). 외부 호출 URL/파라미터를 로깅하기 전에 호출한다.
 * 전역 Logback 컨버터 대신 호출 지점에서 명시적으로 마스킹한다(범위 명확).
 */
public final class LogMaskingUtil {

    private static final String MASK = "***";
    // serviceKey=... (& 또는 문자열 끝까지)
    private static final Pattern SERVICE_KEY = Pattern.compile("(serviceKey=)[^&\\s]+");

    private LogMaskingUtil() {
    }

    /** URL/쿼리 문자열 내 serviceKey 값을 마스킹한다. null 입력은 null 반환. */
    public static String maskServiceKey(String urlOrQuery) {
        if (urlOrQuery == null) {
            return null;
        }
        return SERVICE_KEY.matcher(urlOrQuery).replaceAll("$1" + MASK);
    }
}
