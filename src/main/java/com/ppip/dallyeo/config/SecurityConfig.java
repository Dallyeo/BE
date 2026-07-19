package com.ppip.dallyeo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Security 껍데기 (US-AUTH-4 부분). U1-a는 인증 대상(🔒) 엔드포인트가 없어 전체 permitAll.
 * 세션 미사용(stateless).
 *
 * <p>U1-b(U4 착수 시) 전환 예정:
 * <ul>
 *   <li>deny-by-default + 공개 화이트리스트로 정교화</li>
 *   <li>JwtAuthenticationFilter 추가</li>
 * </ul>
 */
@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                // TODO(U1-b): deny-by-default + 화이트리스트 + JwtAuthenticationFilter
                .authorizeHttpRequests(auth -> auth.anyRequest().permitAll());
        return http.build();
    }
}
