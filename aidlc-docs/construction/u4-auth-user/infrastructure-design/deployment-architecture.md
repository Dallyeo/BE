# Deployment Architecture (Light) — U4 인증·사용자 🔒

> U1-a/U2/U3 토폴로지 상속 + U4: users 테이블, Redis refresh, 소셜(Kakao/Apple) egress, JWT/보안 필터.

---

## 1. 토폴로지

```
              [ Client / Frontend ]
                     │ HTTPS :443
                     ▼
              [ nginx (TLS 종단) ]  ── deploy/nginx
                     │ http :8080
                     ▼
  ┌───────────────────────────────────────────────┐
  │ AWS EC2 (single, 코로케이션)                    │
  │  Spring Boot 앱 (java -jar, systemd)            │
  │   - SecurityConfig(deny-by-default+화이트리스트) │
  │   - JwtAuthenticationFilter → SecurityContext   │
  │   - AuthController/AuthService                   │
  │   - OAuthClientResolver → Kakao / Apple         │
  │   - UserController/UserService                   │
  │        │ localhost:3306                         │
  │        ▼                                        │
  │   MySQL: users(+course+badge) 테이블             │
  │   Redis:6379 ── refresh:{userId}(해시,7d)        │
  │              └─ Apple JWKS 캐시(6h)              │
  └───────────────┬───────────────────────────────┘
                  │ outbound HTTPS :443
                  ├──▶ [ kapi.kakao.com ]  (사용자 조회)
                  ├──▶ [ appleid.apple.com ] (JWKS)
                  └──▶ [ apis.data.go.kr ]  (TourAPI, U3 상속)
```

## 2. 기동 시퀀스 (U4)
```
앱 부팅
 → env 검증: JWT_SECRET 필수(없으면 fail-fast)
 → JPA: users 테이블 자동 생성(ddl-auto=update), (provider,providerUserId) 유니크
 → SecurityConfig: permitAll 껍데기(U1-a) → deny-by-default + 화이트리스트로 교체 적용
 → 서빙:
     🌐 /auth/login/{provider}, /auth/refresh (+ 기존 공개 regions/courses/places)
     🔒 /auth/logout, /users/me (GET/PATCH/DELETE) — 토큰 필요
```

## 3. 배포 절차 (경량)
- fat JAR(신규 의존성 jjwt/nimbus 포함) → EC2 → systemd restart.
- **신규 인프라 없음**. 확인 항목:
  - `JWT_SECRET`, `APPLE_CLIENT_ID` env 설정(`.env`/systemd EnvironmentFile).
  - 아웃바운드 443: kapi.kakao.com / appleid.apple.com 도달성.
  - nginx TLS 종단 정상(in-transit).

## 4. 리스크 / 후속
- **소셜 외부 의존**: Kakao/Apple 장애 시 로그인 불가 → Resilience4j(401 매핑) + Apple JWKS 캐시로 완화.
- **JWT_SECRET 유출 시** 전 토큰 위조 가능 → env 관리 철저, 유출 시 시크릿 로테이션(전 사용자 재로그인).
- **단일 세션(회전)**: 다기기 동시 로그인 시 이전 기기 로그아웃 — 의도된 동작(Q6-FD=A).
- ddl-auto=update / SPOF / at-rest 미암호화 → U1-a~U3와 동일(deferred, accepted risk).
