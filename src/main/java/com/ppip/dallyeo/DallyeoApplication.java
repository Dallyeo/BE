package com.ppip.dallyeo;

import jakarta.annotation.PostConstruct;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DallyeoApplication {

    /** 서비스 기준 시간대. 사용자가 전부 국내라 서버 시각·로그·날짜 경계를 한국시간으로 맞춘다. */
    public static final String ZONE = "Asia/Seoul";

    public static void main(String[] args) {
        SpringApplication.run(DallyeoApplication.class, args);
    }

    /**
     * JVM 기본 시간대를 한국시간으로 고정한다.
     *
     * <p>EC2 등 배포 환경의 기본값은 보통 UTC라, 그대로 두면 로그 시각과
     * {@code LocalDate.now()} 같은 '오늘' 판정이 9시간 어긋난다.
     *
     * <p>저장된 시각이 밀리지는 않는다 — 시각 컬럼은 전부 {@code Instant}(절대시각)이고,
     * DB 변환 기준도 JDBC URL의 {@code serverTimezone=Asia/Seoul}로 이미 고정돼 있어
     * JVM 기본값에 의존하지 않는다.
     */
    @PostConstruct
    void useKoreanTime() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZONE));
    }
}
