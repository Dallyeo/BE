# Code Summary — U6 업적 (Achievement) 🔒

> 신규 도메인. 러닝 기록(U5) 저장 시 자동 판정 + 수동 unlock + 목록 조회. NFR/Infra는 U1-a/U4/U5 상속(신규 의존성·시크릿·인프라 없음, `user_achievement` 테이블만 추가).

## 생성 파일 (신규, `src/main/java/com/ppip/dallyeo/achievement/`)
| 파일 | 역할 |
|---|---|
| `AchievementType.java` | 업적 카탈로그 enum 8종 + 조건(kind/region/courseId) + `fromCode` |
| `UserAchievement.java` | 달성 기록 엔티티(`user_achievement`, `(userId,achievement)` 유니크, @PrePersist unlockedAt) |
| `UserAchievementRepository.java` | findByUserId, existsByUserIdAndAchievement |
| `AchievementService.java` | evaluateAndUnlock(자동)/unlock(수동)/list. courseId 기반 판정, 지역 코스 집합은 course 실데이터 |
| `AchievementController.java` | `/achievements` GET(목록) / POST `/{code}/unlock` |
| `dto/AchievementResponse.java` | code/name/description/unlocked/unlockedAt |

## 수정 파일
| 파일 | 변경 |
|---|---|
| `run/RunRepository.java` | `findDistinctCourseIds(userId)` 추가(업적 판정용) |
| `run/RunService.java` | 생성자에 `AchievementService` 주입, `save()`에서 저장 후 `evaluateAndUnlock(userId)` 호출(자동 판정). 응답 형태 U5 유지 |
| `run/RunServiceTest.java` | 생성자 3인자 반영(AchievementService mock) |

## 변경 없음
- SecurityConfig(`/achievements/**` deny-by-default 자동), build.gradle(의존성 0), application.properties, Course/User/Run 스키마(run 변경 없음 — 판정은 조회만).

## 검증
- 컴파일 성공, **전체 테스트 125건 통과**(U6 신규 10건: AchievementServiceTest 8 + AchievementControllerTest 2 포함, 0 실패).
- 라이브 e2e(dev 프로파일): 토큰없음 401 / dev로그인 / 초기 8개 미달성 / `POST /runs`(courseId=gunsan-jjamppong-run) → **GUNSAN_BEGINNER+JJAMPPONG 자동 달성** / 수동 unlock 미충족 409 / 없는 코드 404 — 전부 확인. 스모크 데이터 정리 완료.

## 후속(백로그)
- 업적 진행률(%) 표시, 업적 해제/취소, 러닝 저장 응답에 신규 달성 업적 포함(현재는 GET /achievements로 확인), 계정삭제 시 user_achievement cascade.
