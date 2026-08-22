# Security Test Instructions — Dallyeo (Security Baseline 활성)

## Purpose
Security Baseline 확장(활성)에 대한 검증. 인증/인가·입력검증·시크릿·의존성 중심.

## Test Scenarios

### 1. 인증/인가 (SECURITY-08, A01)
- `/runs/**`, `/users/me`, `/auth/logout` 토큰 없이 → **401** (deny-by-default). — `/runs` 401 **확인됨**.
- 타인 리소스 접근: 타인 러닝 `GET /runs/{id}` → **404 은폐**, 사용자 코스 없음(코스 생성 백엔드 제거).
- 토큰 type 교차사용 차단(access↔refresh), 만료 토큰 → 401 (U4 검증됨).

### 2. 입력 검증 (SECURITY-05)
- `POST /runs`: polyline 빈배열/distance·duration ≤0/시간역전/필수누락 → **400**.
- 잘못된 `from`/`to` 날짜 → 400. 잘못된 region/distance/category → 400(U2/U3).

### 3. 시크릿/민감정보 (SECURITY-03/12)
- `JWT_SECRET`·DB 비밀번호·소셜 credential은 env only(VCS 금지). 로그 마스킹(LogMaskingUtil).
- **위치정보 프라이버시**: 러닝 polyline 좌표는 로그 본문 미출력(집계/식별자만).

### 4. 의존성 취약점 (SECURITY-10)
```bash
# 선택: OWASP Dependency-Check 또는 gradle 플러그인으로 스캔(현재 미도입)
./gradlew dependencies --configuration runtimeClasspath | less
```
- 신규 의존성 없음(U5) → 신규 취약 표면 추가 없음.

### 5. 전송/저장 암호화 (SECURITY-01)
- in-transit: nginx TLS 종단(배포).
- at-rest: 단일 EC2 로컬디스크 = **accepted risk(문서화)**. 위치정보(polyline) 저장 → RDS 이관 시 암호화 우선 대상.

## 실행 방법
- 시나리오 1·2는 curl/Postman으로 재현(에러 케이스 폴더 포함).
- 자동 보안 스캐너(정적/의존성)는 후속 도입 권장.

## 결론
- Security Baseline 적용 항목 **준수**, 일부(at-rest 암호화·레이트리밋·자동 취약점 스캔)는 **문서화된 defer/accepted risk**.
