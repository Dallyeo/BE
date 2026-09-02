-- 코스 경유지(waypoint_anchors) 정리 백필 — courses.json 기준.
-- 대상: 자동 생성된 '경유지N' 제거(4개 코스), '경유지(청연루 입구/출구)' → '청연루 입구/출구' 개명(1개 코스).
-- polyline / cumulative_meters / total_meters 는 손대지 않음 (지도 경로 불변).
-- waypoint_count 는 목록 응답값이라 함께 갱신.
-- 실행: mysql -u root -p dallyeo < update-course-waypoints.sql
START TRANSACTION;
-- 짬뽕런: 4개 — 군산수송공원, 경암동철길마을, 조선은행, 짬뽕특화거리
UPDATE course SET waypoint_anchors = '[{"name":"군산수송공원","polylineIndex":0},{"name":"경암동철길마을","polylineIndex":98},{"name":"조선은행","polylineIndex":172},{"name":"짬뽕특화거리","polylineIndex":180}]', waypoint_count = 4 WHERE id = 'gunsan-jjamppong-run';
-- 선유도 해변 런: 3개 — 옥돌해변, 선유도 방파제, 몽돌해변
UPDATE course SET waypoint_anchors = '[{"name":"옥돌해변","polylineIndex":0},{"name":"선유도 방파제","polylineIndex":137},{"name":"몽돌해변","polylineIndex":220}]', waypoint_count = 3 WHERE id = 'gunsan-seonyudo-beach-run';
-- 한옥마을 둘레길 코스: 5개 — 전주경기전 정문, 한벽당, 청연루 입구, 청연루 출구, 전동성당
UPDATE course SET waypoint_anchors = '[{"name":"전주경기전 정문","polylineIndex":0},{"name":"한벽당","polylineIndex":49},{"name":"청연루 입구","polylineIndex":86},{"name":"청연루 출구","polylineIndex":93},{"name":"전동성당","polylineIndex":110}]', waypoint_count = 5 WHERE id = 'jeonju-hanok-village-run';
-- 아중호수 둘레길 코스: 2개 — 전주경기전 정문, 도착지
UPDATE course SET waypoint_anchors = '[{"name":"전주경기전 정문","polylineIndex":0},{"name":"도착지","polylineIndex":272}]', waypoint_count = 2 WHERE id = 'jeonju-ajung-lake-run';
-- 전주동물원 코스: 2개 — 덕진공원 3층 석탑, 전주동물원
UPDATE course SET waypoint_anchors = '[{"name":"덕진공원 3층 석탑","polylineIndex":0},{"name":"전주동물원","polylineIndex":201}]', waypoint_count = 2 WHERE id = 'jeonju-zoo-run';
COMMIT;

-- 검증: SELECT id, waypoint_count, waypoint_anchors FROM course ORDER BY id;
