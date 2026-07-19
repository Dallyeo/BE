# NFR Design Patterns (Light) — U2 지역·코스조회 🌐

> 확정: D1=A(AttributeConverter), D2=B(waypointCount 저장 컬럼). 외부 회복성·캐시 없음 → 영속/조회/검증 패턴만.

---

## 1. 영속성 패턴 (Persistence)
- **JPA AttributeConverter(@Convert)** (D1=A): `polyline`/`cumulativeMeters`/`waypointAnchors`는 엔티티에서 **객체 필드**(List<PolylinePoint>, List<Integer>, List<WaypointAnchor>)로 두고, 각 컨버터가 DB의 JSON TEXT ↔ 객체를 자동 변환.
  - `PolylineConverter`, `IntListConverter`, `WaypointAnchorsConverter` (Jackson3 ObjectMapper 사용).
  - 변환 실패 → 런타임 예외(로그) → GlobalExceptionHandler INTERNAL_ERROR(데이터 무결성 문제 가시화).
- **Enum 저장**: `region`/`declaredCategory`/`measuredCategory`는 `@Enumerated(STRING)`.
- **인덱스**(R2): `@Table(indexes = { region, measuredCategory })`.

## 2. 조회 패턴 (Query)
- **목록(요약)**: `CourseRepository`에서 필터(region/measuredCategory) + 정렬(totalMeters ASC). 요약 매핑에 **경로 JSON 컬럼을 건드리지 않음**.
  - `waypointCount`는 **저장 컬럼**(D2=B) → 목록 조회 시 waypointAnchors 역직렬화 불필요(성능).
  - 구현: `List<Course> findByRegionAndMeasuredCategory(...)` 계열 + null 조건 처리(Specification 또는 조건 분기), 또는 소량이라 전체 조회 후 메모리 필터도 허용.
- **상세**: `findById` → AttributeConverter가 경로 객체 자동 역직렬화 → CourseDetail 매핑. 미존재 → 404.

## 3. 검증 패턴 (Validation) — 보안 상속
- 컨트롤러 파라미터 검증: `region`/`distance` 문자열 → enum 파싱, 실패 시 400(BR-U2-4).
- path `id`는 String, 미존재는 404(존재검증은 서비스).

## 4. 시드 적재 패턴 (Seeding)
- `ApplicationRunner`로 기동 시 1회 실행. classpath `data/courses.json` → 파싱 → id 멱등 삽입(existsById 스킵).
- 트랜잭션: 코스 단위 저장(개별 실패 격리, WARN+스킵).
- `waypointCount`는 적재 시 anchors 개수로 계산해 컬럼 저장(D2=B).

## 5. 관측성 (Observability)
- 시드 결과 INFO(신규/스킵 수), 변환 실패 WARN. U1-a 구조화 로깅 상속.

## 6. N/A (U2)
- Resilience4j/서킷·재시도, 캐시, 스레드풀(벌크헤드), 외부 시크릿 — 이 유닛 미해당.
