# Integration Test Instructions — Dallyeo

## Purpose
모놀리식 단일 앱 내 논리 유닛(U1-a~U5) 간 상호작용 및 실제 인프라(MySQL/Redis/외부 API) 연동을 검증한다.

## 사전 조건
- MySQL(`dallyeo`), Redis 기동. 유효한 `JWT_SECRET`.
- 앱 기동: `./gradlew bootRun` 또는 `java -jar build/libs/Dallyeo-0.0.1-SNAPSHOT.jar`

## Test Scenarios

### 시나리오 1: 부팅/컨텍스트 로드 (U1-a~U5 전체 배선)
- **검증**: 전체 Spring 컨텍스트 로드 = Jackson3 + jjwt(Jackson2 내장) 공존, 보안 필터 체인, JPA 엔티티(users/course/badge/region/**run**) 스키마.
- **실행**: `DallyeoApplicationTests`(@SpringBootTest) — **통과 확인됨**.
- **기대**: 컨텍스트 로드 성공, `run` 테이블·`idx_run_user_finished`·`course.description` 생성.

### 시나리오 2: 인증(U4) → 보호 리소스(U5) deny-by-default
- **검증**: `/runs/**`가 토큰 없이 거부되는지.
- **실행(라이브)**:
  ```bash
  curl -s -o /dev/null -w "%{http_code}\n" localhost:8080/runs   # 기대: 401
  ```
- **결과**: **401 확인됨**(U1-b deny-by-default가 U5 신규 엔드포인트에 자동 적용).

### 시나리오 3: 로그인(U4) → 러닝 저장/조회(U5) 소유권 흐름
- **Setup**: 개발용 로그인으로 토큰 확보(`POST /dev/login`, dev 프로파일) → `accessToken`.
- **Steps**:
  1. `POST /runs`(Bearer) → 201, 응답 `id` 확보.
  2. `GET /runs`(Bearer) → 본인 기록 목록에 포함, polyline 미포함(경량).
  3. `GET /runs/{id}`(Bearer) → 상세(polyline 포함).
  4. 타인 토큰으로 `GET /runs/{id}` → **404**(소유권 은폐).
- **기대**: 소유권 격리, courseId 느슨 참조 시 courseName best-effort.
- **참고**: Postman 컬렉션 "러닝 기록 (Runs) 🔒" 폴더로 재현 가능(runId 자동 캡처).

### 시나리오 4: 코스 조회(U2) description 연동
- **실행(라이브)**:
  ```bash
  curl -s localhost:8080/courses | python3 -m json.tool | head
  ```
- **결과**: 목록/상세에 `description` 노출 **확인됨**(DB 백필 후).

## Setup / 실행
```bash
# 1) 인프라 기동
#    MySQL, Redis 로컬 기동
# 2) 앱 기동
./gradlew bootRun
# 3) 위 시나리오 curl / Postman 실행
```

## Cleanup
```bash
# 테스트 러닝 데이터 정리(선택)
mysql -u root -p dallyeo -e "DELETE FROM run WHERE userId = <테스트 사용자 id>;"
```

## 자동화 노트
- 현재 통합 시나리오는 수동/스모크(curl·Postman) 중심. 자동 통합 테스트(@SpringBootTest + Testcontainers)는 후속 도입 가능.
