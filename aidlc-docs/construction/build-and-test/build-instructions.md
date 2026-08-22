# Build Instructions — Dallyeo

## Prerequisites
- **Build Tool**: Gradle (wrapper `./gradlew`), Spring Boot
- **JDK**: Java 17
- **런타임 의존 서비스**: MySQL 8.x(`localhost:3306/dallyeo`), Redis(`localhost:6379`) — 부팅/통합 시 필요(순수 단위테스트는 불필요)
- **환경변수/시크릿** (`.env` 또는 `application-secret.properties`):
  - `DB_USERNAME`, `DB_PASSWORD`
  - `JWT_SECRET`(필수, 공백이면 기동 실패), `APPLE_CLIENT_ID`
  - TourAPI 키(장소 도메인 실사용 시)
- **System**: macOS/Linux, 메모리 2GB+

## Build Steps

### 1. 의존성 해석 + 컴파일
```bash
./gradlew compileJava compileTestJava
```
- 신규 의존성 없음(U5는 의존성 추가 0). jjwt 0.12.6 + nimbus-jose-jwt(U4)만 유지.

### 2. 환경 구성
```bash
# 로컬: application-secret.properties 에 DB_USERNAME/DB_PASSWORD, JWT_SECRET 등 설정
# 배포: systemd EnvironmentFile 또는 .env 로 주입
```

### 3. 전체 빌드(테스트 포함)
```bash
./gradlew build
# 또는 패키징만: ./gradlew bootJar
```

### 4. 빌드 성공 확인
- **산출물**: `build/libs/Dallyeo-0.0.1-SNAPSHOT.jar` (fat JAR, 약 78MB) — 확인됨.
- **허용 경고**: `OpenJDK ... Sharing is only supported ...`(CDS 경고, 무해).

## Troubleshooting

### 컴파일 오류
- **원인**: DTO(record) 시그니처 변경 후 생성자 호출 미갱신(예: CourseSummary/CourseDetail에 description 추가).
- **해결**: 해당 record를 `new`로 생성하는 모든 위치(테스트 포함) 인자 보정.

### 기동 실패(fail-fast)
- **원인**: `JWT_SECRET` 미설정/공백.
- **해결**: env에 유효한 시크릿 설정.

### DB 연결 실패
- **원인**: MySQL 미기동 또는 `dallyeo` 스키마 없음.
- **해결**: MySQL 기동, `CREATE DATABASE dallyeo;`(테이블은 `ddl-auto=update`가 자동 생성).
