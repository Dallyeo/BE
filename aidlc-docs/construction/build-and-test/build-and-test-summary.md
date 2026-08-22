# Build and Test Summary — Dallyeo

> 실행일: 2026-08-22. 전 유닛(U1-a · U2 · U3 · U4 · U5) 코드 완료 후 통합 빌드/테스트.

## Build Status
- **Build Tool**: Gradle (Spring Boot), Java 17
- **Build Status**: ✅ Success
- **Artifacts**: `build/libs/Dallyeo-0.0.1-SNAPSHOT.jar` (fat JAR, 약 78MB)
- **신규 의존성**: 없음(U5)

## Test Execution Summary

### Unit Tests
- **총 테스트**: 115
- **Passed**: 115
- **Failed**: 0 / **Errors**: 0 / **Skipped**: 0
- **테스트 클래스**: 31개
- **Status**: ✅ PASS
- **커버리지**: JaCoCo 미도입(측정 안 함)

### Integration Tests (스모크/부팅)
- **부팅/컨텍스트 로드**(`DallyeoApplicationTests` @SpringBootTest): ✅ PASS — Jackson3+jjwt 공존, 보안 필터, JPA(users/course/badge/region/**run**) 스키마.
- **deny-by-default**: `GET /runs` 무토큰 → **401** ✅
- **코스 description 연동**: `GET /courses` 응답에 description 포함 ✅
- **Status**: ✅ PASS (수동/라이브 스모크)

### Performance Tests
- **Status**: N/A (하드 SLA 미설정 — 정식 부하테스트 defer, 관측 스모크만)

### Security Tests (Security Baseline)
- 인증/인가(401·소유권 404), 입력검증(400), 시크릿 env·로그 마스킹, 위치정보 미로깅: ✅ 준수
- at-rest 암호화 / 레이트리밋 / 자동 취약점 스캔: 문서화된 defer·accepted risk
- **Status**: ✅ PASS (baseline), 일부 defer

## Overall Status
- **Build**: ✅ Success
- **All Tests**: ✅ Pass (115/115, 부팅·deny-by-default·description 라이브 확인)
- **Ready for Operations**: Yes

## 생성 문서
- build-instructions.md
- unit-test-instructions.md
- integration-test-instructions.md
- performance-test-instructions.md
- security-test-instructions.md
- build-and-test-summary.md

## Next Steps
- 전 테스트 통과 → Operations 단계(배포/운영, 현재 워크플로우상 placeholder)로 진행 가능.
- 후속 백로그: 러닝 수정/삭제·완주율·통계, 목록 페이징, polyline 크기 상한, 계정삭제 시 runs cascade, JaCoCo/자동 통합·부하·보안 스캔 도입.
