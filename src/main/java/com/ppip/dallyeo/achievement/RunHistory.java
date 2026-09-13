package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.run.Run;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 업적 판정에 필요한 러닝 이력 집계. 사용자의 러닝을 <b>한 번만</b>훑어 만든다
 * (업적이 21종이라 매 업적마다 다시 세면 같은 목록을 21번 돌게 된다).
 *
 * <p>월·시(時) 판정은 <b>한국시간(KST)</b> 기준이다. 러닝 시각은 UTC(Instant)로 저장되므로
 * 그대로 쓰면 "아침 8시 이전"이 한국 사용자 기준과 9시간 어긋난다.
 */
public record RunHistory(
        Set<String> courseIds,
        int runCount,
        long totalDistanceMeters,
        int longestDurationSeconds,
        boolean hasFreeRun,
        Set<Integer> finishMonths,
        int earliestStartHour
) {

    /** 업적 판정 기준 시간대. */
    public static final ZoneId ZONE = ZoneId.of("Asia/Seoul");

    /** 러닝이 하나도 없을 때의 '시작 시각 없음' 표시값(어떤 시(時) 조건에도 걸리지 않도록). */
    private static final int NO_START = 24;

    public static RunHistory of(List<Run> runs) {
        Set<String> courseIds = new HashSet<>();
        Set<Integer> finishMonths = new HashSet<>();
        long totalDistance = 0;
        int longestDuration = 0;
        boolean hasFreeRun = false;
        int earliestStartHour = NO_START;

        for (Run run : runs) {
            if (run.getCourseId() == null) {
                hasFreeRun = true;
            } else {
                courseIds.add(run.getCourseId());
            }
            totalDistance += run.getDistanceMeters();
            longestDuration = Math.max(longestDuration, run.getDurationSeconds());
            if (run.getFinishedAt() != null) {
                finishMonths.add(LocalDateTime.ofInstant(run.getFinishedAt(), ZONE).getMonthValue());
            }
            if (run.getStartedAt() != null) {
                earliestStartHour = Math.min(earliestStartHour,
                        LocalDateTime.ofInstant(run.getStartedAt(), ZONE).getHour());
            }
        }
        return new RunHistory(courseIds, runs.size(), totalDistance, longestDuration,
                hasFreeRun, finishMonths, earliestStartHour);
    }

    /**
     * 한국시간 기준 자정~{@code hour}시 사이에 시작한 러닝이 있는지.
     * 가장 이른 시작 '시(時)'만 보면 되므로 0시가 가장 이른 값이다(자정부터가 기준).
     */
    public boolean startedBefore(int hour) {
        return earliestStartHour < hour;
    }

    /** 한국시간 기준 해당 월에 완주한 러닝이 있는지. */
    public boolean finishedInMonth(int month) {
        return finishMonths.contains(month);
    }
}
