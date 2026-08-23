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
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

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
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .httpBasic(basic -> basic.disable())
                .formLogin(form -> form.disable())
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // CORS preflight(OPTIONS)는 인증 없이 통과 (deny-by-default 하에서 필수)
                        .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
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

    /**
     * ⚠️ 임시(TEMPORARY): 전체 오리진 허용. iOS 웹뷰 CORS 차단 우선 해제용(테스트 목적).
     *
     * <p>안전 근거: 인증이 Bearer 토큰(헤더) 방식이고 쿠키 세션을 쓰지 않으므로
     * {@code allowCredentials=false}인 와일드카드 허용은 CSRF를 유발하지 않는다.
     * 보호 엔드포인트는 토큰 없으면 401이라 데이터 유출 위험 낮음.
     *
     * <p><b>TODO(보안): 테스트 후 반드시 좁힐 것.</b> 실제 웹뷰 Origin으로 제한:
     * 예) {@code https://dallyeo.cloud}, {@code capacitor://*}, {@code ionic://*}, {@code http://localhost:*}.
     * 절대 {@code allowCredentials(true)}와 와일드카드/Origin 반사를 함께 쓰지 말 것.
     * 상세: aidlc-docs/backlog.md, deploy 후 프론트 Origin 확인해서 확정.
     */
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of("*"));   // TEMP: 전체 허용
        config.setAllowedMethods(List.of("GET", "POST", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(false);               // 토큰(헤더) 인증 → 쿠키 credentials 불필요
        config.setMaxAge(3600L);                          // preflight 캐시 1h
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
