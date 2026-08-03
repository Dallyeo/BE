package com.ppip.dallyeo.config;

import com.ppip.dallyeo.auth.JwtAuthenticationFilter;
import com.ppip.dallyeo.auth.JwtProperties;
import com.ppip.dallyeo.auth.RestAccessDeniedHandler;
import com.ppip.dallyeo.auth.RestAuthenticationEntryPoint;
import com.ppip.dallyeo.external.oauth.OAuthProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * 보안 설정 (US-AUTH-4, U1-b). U1-a permitAll 껍데기 → deny-by-default + 공개 화이트리스트로 교체.
 * 세션 미사용(stateless). JwtAuthenticationFilter 등록, 401/403 공통 응답 처리기 배선.
 *
 * <p>화이트리스트 근거: auth-classification.md §1 (읽기 전용 공개 데이터 + 토큰 발급 전 인증 흐름).
 * 그 외(logout/users/**, 향후 runs/**·POST courses)는 전부 인증 필요.
 */
@Configuration
@EnableConfigurationProperties({JwtProperties.class, OAuthProperties.class})
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http,
                                           JwtAuthenticationFilter jwtAuthenticationFilter,
                                           RestAuthenticationEntryPoint authenticationEntryPoint,
                                           RestAccessDeniedHandler accessDeniedHandler) throws Exception {
        http
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // 🌐 공개: 토큰 발급 전 인증 흐름
                        .requestMatchers(HttpMethod.POST, "/auth/login/*", "/auth/refresh").permitAll()
                        // 🌐 공개: 비개인 참조/조회 데이터
                        .requestMatchers(HttpMethod.GET, "/regions", "/courses", "/courses/*", "/places/**").permitAll()
                        // 인프라: 헬스체크(배포 스위치 판단)
                        .requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
                        // 개발/테스트 전용 로그인 — 컨트롤러가 @Profile("dev")라 prod에는 핸들러가 없음(404)
                        .requestMatchers("/dev/**").permitAll()
                        // 그 외 전부 인증(deny-by-default)
                        .anyRequest().authenticated())
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
