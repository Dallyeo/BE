# Code Summary — U5 러닝기록 🔒

> Code Generation Part 2 실행 결과. 스토리 US-RUN-1/2/3. 사용자 코스 생성(US-COURSE-3)은 백엔드 제거로 미구현.

---

## 생성 파일 (Application Code, workspace root)

### 신규 (run 도메인)
| 파일 | 역할 |
|---|---|
| `src/main/java/com/ppip/dallyeo/run/Run.java` | 엔티티. Long id(IDENTITY), userId, courseId(nullable), polyline(MEDIUMTEXT, PolylineConverter 재사용), distance/duration/averagePace, startedAt/finishedAt/createdAt(@PrePersist), `(userId, finishedAt)` 인덱스 |
| `src/main/java/com/ppip/dallyeo/run/RunRepository.java` | `JpaRepository<Run, Long>` + `findByOwnerAndPeriod`(기간 optional, finishedAt DESC) |
| `src/main/java/com/ppip/dallyeo/run/RunService.java` | 저장(검증·userId 귀속)/목록(격리·기간 파싱)/상세(소유권 404), courseName best-effort lookup, completionRate=null |
| `src/main/java/com/ppip/dallyeo/run/RunController.java` | `/runs` — POST(201)/GET/GET{id}, `@AuthUser`, `ApiResponse` 래핑 |
| `src/main/java/com/ppip/dallyeo/run/dto/RunCreateRequest.java` | record + jakarta validation(@NotEmpty polyline, @Positive distance/duration, @NotNull) |
| `src/main/java/com/ppip/dallyeo/run/dto/RunSummaryResponse.java` | record. 목록 요약(polyline 제외) |
| `src/main/java/com/ppip/dallyeo/run/dto/RunDetailResponse.java` | record. 상세(polyline 포함, completionRate=null) |

### 테스트
| 파일 | 내용 | 건수 |
|---|---|---|
| `src/test/java/com/ppip/dallyeo/run/RunServiceTest.java` | 저장(성공/completionRate null/courseId 느슨/시간역전 400), 목록(격리·courseName·날짜 400), 상세(본인/타인 404/미존재 404) | 8 |
| `src/test/java/com/ppip/dallyeo/run/RunControllerTest.java` | POST/GET/GET{id} 위임·래핑 | 3 |

## 수정 파일 (문서/설정)
| 파일 | 변경 |
|---|---|
| `API.md` | §7 러닝 기록(Runs) 추가(7.1~7.3), 헤더/미구현 표 갱신(코스 생성=프론트 담당 명시) |
| `dallyeo-postman-collection.json` | "러닝 기록 (Runs) 🔒" 폴더 3요청 추가(POST test 스크립트로 runId 캡처), 컬렉션명 U5 반영 |

## 변경 없음 (계획대로)
- `SecurityConfig`(deny-by-default로 `/runs/**` 자동 authenticated), `build.gradle`(신규 의존성 0), `application.properties`, `Course`/`User` 엔티티.

## 재사용
- `PolylinePoint`/`PolylineConverter`(U2), `CourseRepository`(U2, courseName lookup), `@AuthUser`/`AuthUserArgumentResolver`(U4), `ApiResponse`/`BusinessException`/`ErrorCode`/`GlobalExceptionHandler`(U1-a).

## 검증 결과
- `./gradlew compileJava compileTestJava` — 성공.
- U5 단위 테스트 **11건 전부 통과**(RunServiceTest 8 + RunControllerTest 3, 실패 0).
- 부팅/통합(테이블 생성·deny-by-default 401·MEDIUMTEXT DDL) 검증은 Build & Test 단계.

## 스토리 완료
- ✅ US-RUN-1 완료 러닝 기록 저장
- ✅ US-RUN-2 러닝 기록 목록 조회
- ✅ US-RUN-3 러닝 기록 상세 조회

## 후속(백로그)
- 러닝 수정/삭제, 완주율 계산, 통계/집계, 목록 페이징, polyline 크기 상한, 계정삭제 시 runs cascade 정책.
