# Unit Test Execution — Dallyeo

## Run Unit Tests

### 1. 전체 단위 테스트 실행
```bash
./gradlew test
```
- 유닛별 실행: `./gradlew test --tests "com.ppip.dallyeo.run.*"`(U5), `"com.ppip.dallyeo.course.*"`(U2 등).

### 2. 결과 확인 (2026-08-22 실행 기준)
- **총 테스트: 115 / 실패: 0 / 에러: 0 / 스킵: 0** (테스트 클래스 31개) — **PASS**
- 유닛별 대표:
  - U5 run: `RunServiceTest`(8) + `RunControllerTest`(3) = 11
  - U2 course: Controller/QueryService/DataLoader/Distance/Converters = 15 (description 추가 후 재확인 통과)
  - U4 auth/user: Jwt/Auth/RefreshTokenStore/User/OAuth 등 다수
  - `DallyeoApplicationTests`(@SpringBootTest): 컨텍스트 로드 = **전체 부팅 검증**
- **리포트 위치**: `build/test-results/test/*.xml`, HTML: `build/reports/tests/test/index.html`

### 3. 실패 시 대응
1. `build/reports/tests/test/index.html`에서 실패 케이스 확인
2. 원인 코드 수정
3. 재실행하여 0 실패 확인

## 커버리지 노트
- 도메인 서비스/컨트롤러 로직은 Mockito 기반 순수 단위테스트로 검증(외부 의존 mock).
- 커버리지 툴(JaCoCo)은 현재 미도입 — 필요 시 후속에서 추가.
