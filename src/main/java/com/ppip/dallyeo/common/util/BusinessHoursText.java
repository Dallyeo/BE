package com.ppip.dallyeo.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/**
 * TourAPI 영업시간 원문 → 개행(\n) 구분 항목 목록.
 *
 * <p>원문에는 정해진 구분자가 없다. 군산 음식점 실데이터만 봐도 여러 형태가 섞여 있다:
 * <pre>
 *   "11:00~21:00&lt;br&gt;- 마지막 주문 20:30"      (br 태그)
 *   "- 12:00~21:00- 준비시간 14:00~17:00"      (글머리표가 앞 항목에 붙음)
 *   "- 11:00~21:30 - 준비시간 14:00~16:30"     (글머리표 앞뒤 공백)
 *   "12:00~21:00 (준비시간 14:00~17:00)"       (괄호)
 *   "08:00~16:00※ 재료 소진 시 영업 종료"        (※ 주석)
 * </pre>
 * 그대로 넘기면 클라이언트 카드 한 줄에 다 밀려 들어가 잘리므로("- 12:00~21:00- 준비시간 14:00~1..."),
 * 위 구분들을 개행으로 통일한다. 카드용 대표 한 줄은 {@link #first}.
 *
 * <p><b>주의</b>: 하이픈은 구분자이면서 동시에 시간 범위 표기이기도 하다("11:00-23:00").
 * 그래서 시간-하이픈-시간 패턴을 먼저 보호한 뒤에 글머리표를 자른다.
 */
public final class BusinessHoursText {

    /** {@code <br>}, {@code <br/>}, {@code </p>} 등 줄바꿈 성격의 태그. */
    private static final Pattern LINE_BREAK_TAG =
            Pattern.compile("(?i)<\\s*br\\s*/?\\s*>|<\\s*/\\s*(p|div|li|tr)\\s*>");

    /** 나머지 HTML 태그. */
    private static final Pattern ANY_TAG = Pattern.compile("<[^>]+>");

    /** 시간 범위의 하이픈("11:00-23:00")은 구분자가 아니므로 잠시 치환해 보호한다. */
    private static final Pattern TIME_RANGE_HYPHEN =
            Pattern.compile("(\\d{1,2}:\\d{2})\\s*-\\s*(\\d{1,2}:\\d{2})");
    private static final String HYPHEN_GUARD = "\u0001";

    /** 항목 글머리표: 하이픈 뒤에 공백이 오는 경우(보호된 시간 범위는 여기 걸리지 않는다). */
    private static final Pattern BULLET = Pattern.compile("[ \t]*[-–—·•]+[ \t]+");

    /** 시간 뒤에 바로 붙는 괄호 주석: "12:00~21:00 (준비시간 …)" → 다음 줄로. */
    private static final Pattern PAREN_NOTE = Pattern.compile("(?<=\\d{2}:\\d{2})[ \t]*(?=\\()");

    /** ※ 주석도 앞 항목에 붙어 오는 경우가 많다. */
    private static final Pattern MARK_NOTE = Pattern.compile("[ \t]*(?=※)");

    /** 줄 앞에 남은 글머리표. */
    private static final Pattern LEADING_BULLET = Pattern.compile("^[-–—·•*]+[ \t]*");

    /** 시간 표기 포함 여부 — 대표값을 고를 때 "[평일]" 같은 머리글 줄을 건너뛰는 데 쓴다. */
    private static final Pattern TIME = Pattern.compile("\\d{1,2}:\\d{2}");

    private BusinessHoursText() {
    }

    /**
     * 원문 → 개행 구분 항목 목록. 태그 제거, HTML 엔티티 해제, 구분자 통일, 공백 정리.
     * 비거나 정리 후 남는 내용이 없으면 null.
     */
    public static String normalize(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String s = LINE_BREAK_TAG.matcher(raw).replaceAll("\n");
        s = ANY_TAG.matcher(s).replaceAll("");
        s = unescapeEntities(s).replace('\u00A0', ' ').replace("\r\n", "\n").replace('\r', '\n');

        s = TIME_RANGE_HYPHEN.matcher(s).replaceAll("$1" + HYPHEN_GUARD + "$2");
        s = BULLET.matcher(s).replaceAll("\n");
        s = PAREN_NOTE.matcher(s).replaceAll("\n");
        s = MARK_NOTE.matcher(s).replaceAll("\n");

        List<String> lines = new ArrayList<>();
        for (String line : s.split("\n")) {
            String cleaned = LEADING_BULLET.matcher(line.trim()).replaceFirst("")
                    .replace(HYPHEN_GUARD, "-")
                    .replaceAll("[ \t]{2,}", " ")
                    .trim();
            if (!cleaned.isEmpty()) {
                lines.add(cleaned);
            }
        }
        return lines.isEmpty() ? null : String.join("\n", lines);
    }

    /**
     * 대표 영업시간 한 줄. 시간이 들어 있는 첫 줄을 고른다 —
     * "[평일]" 같은 머리글이 첫 줄인 경우가 있어 무조건 첫 줄을 쓰면 시간이 안 나온다.
     * 시간 표기가 하나도 없으면("상시 개방", "연중무휴") 그냥 첫 줄.
     */
    public static String first(String normalized) {
        if (normalized == null || normalized.isBlank()) {
            return null;
        }
        String[] lines = normalized.split("\n");
        for (String line : lines) {
            if (TIME.matcher(line).find()) {
                return line.trim();
            }
        }
        String head = lines[0].trim();
        return head.isEmpty() ? null : head;
    }

    /** 자주 나오는 HTML 엔티티만 해제(전용 파서를 끌어올 만큼의 일이 아니다). */
    private static String unescapeEntities(String s) {
        return s.replace("&nbsp;", " ")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&quot;", "\"")
                .replace("&#39;", "'")
                .replace("&apos;", "'")
                .replace("&amp;", "&");
    }
}
