# Tech Stack Decisions — U5 러닝기록 🔒

> **신규 기술 요소 없음(NQ3).** U1-a/U2/U4에서 확정된 스택을 그대로 사용.

---

## 상속(변경 없음)
| 영역 | 선택 | 출처 |
|---|---|---|
| 웹/API | Spring Boot Web, `@RestController`, 공통 `ApiResponse<T>` | U1-a |
| 영속 | Spring Data JPA + MySQL (`ddl-auto=update`) | U1-a/U2 |
| JSON 컬럼 | `@Convert` 컨버터 + TEXT 계열 컬럼(U2 `PolylineConverter` 재사용) | U2 |
| 예외 처리 | `GlobalExceptionHandler` + `BusinessException`/`ErrorCode` | U1-a |
| 보안/인증 | JWT(HS256) + `JwtAuthenticationFilter` + deny-by-default SecurityConfig + `@AuthUser` 주입 | U1-b/U4 |
| 로깅 | 구조화 로깅 + 민감정보 마스킹 | U1-a |

## U5 신규(구현 세부, 새 의존성 아님)
| 항목 | 결정 | 근거 |
|---|---|---|
| Run 저장소 | `RunRepository extends JpaRepository<Run, Long>` + 파생/`@Query` 조회(userId+기간+정렬) | 기존 JPA 관례 |
| Run id 생성 | `@GeneratedValue(IDENTITY)` (Long) | User와 동일 관례(FD Q1=A) |
| polyline 컬럼 | `@Convert(PolylineConverter)` + `@Column(columnDefinition = "MEDIUMTEXT")` | 상한 미설정(NQ1) 하 절단 방지 안전장치 |
| 인덱스 | `(userId, finishedAt)` 복합 인덱스 | 목록 조회 성능(본인·기간·최신순) |
| courseName lookup | `CourseRepository`(U2) 읽기 재사용, best-effort | 신규 저장소 불필요 |

## 신규 의존성 / 시크릿 / 외부 연동
- **없음.** build.gradle 의존성 추가 없음. 신규 환경변수/시크릿 없음. 외부 API 호출 없음.
