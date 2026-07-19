# Tech Stack Decisions (Light) — U2 지역·코스조회 🌐

> 기존 스택(Spring Boot 4/JPA/MySQL/Jackson3) 재사용. U2 신규 결정만.

---

## 확정 기술 선택

| 관심사 | 선택 | 근거 |
|---|---|---|
| **영속성** | **Spring Data JPA + MySQL** | 기존 스택. `CourseRepository extends JpaRepository<Course, String>` |
| **경로 데이터 저장** | **JSON 문자열 컬럼(TEXT/LONGTEXT)** | Q1=A. polyline/cumulativeMeters/waypointAnchors를 Jackson3 `ObjectMapper`로 직렬화 |
| **JSON 직렬화** | **Jackson3(tools.jackson) ObjectMapper** | U1-a와 동일 스택. JPA `@Convert` AttributeConverter 또는 서비스 레벨 직렬화 |
| **캐싱** | **미적용** | R1=A. 정적 소량 |
| **인덱스** | `region`, `measuredCategory` 인덱스 | R2=A. `@Table(indexes=...)` |
| **시드 적재** | **ApplicationRunner + classpath 리소스** | 기동 시 `data/courses.json` 로드, id 기준 멱등 삽입(Q4=B) |
| **입력 검증** | Spring Validation | 필터/파라미터 검증 |

## 스키마/설정 메모
- `spring.jpa.hibernate.ddl-auto`: 현재 `update`. U2 `course` 테이블 자동 생성. (운영 정책은 Build & Test에서 재확인.)
- JSON 컬럼: MySQL `TEXT`(polyline ~400점 → 수 KB, LONGTEXT 불필요하나 여유 위해 컬럼 길이 확인).
- `data/courses.json`을 `src/main/resources/data/courses.json`으로 복사(현재 aidlc-docs 하위 → 런타임 classpath로 이동).

## 유보 (범위 밖)
- 코스 캐싱/무효화 전략 → U5 사용자 코스 생성 시 재검토.
- 공간 인덱스/지오쿼리 → 현 범위 아님(반경 검색은 U3 TourAPI).
