package com.ppip.dallyeo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * 외부 TourAPI 병렬 호출 전용 실행기 (D6: 벌크헤드).
 * 서블릿 요청 스레드와 격리 — 외부 호출 지연이 톰캣 워커를 고갈시키지 않도록 한다.
 * 실제 병렬 조합(PlaceDetailAssembler)은 U3에서 이 실행기를 사용한다.
 */
@Configuration
public class AsyncConfig {

    @Bean(name = "tourApiExecutor")
    public Executor tourApiExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(8);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("tourapi-");
        // 포화 시 호출 스레드에서 실행(거부 대신 백프레셔).
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.initialize();
        return executor;
    }
}
