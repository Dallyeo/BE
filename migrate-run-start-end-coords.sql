-- 러닝 기록 구조 변경 (2026-09-13)
--
-- 무엇이 바뀌나:
--   · polyline(전체 경로 좌표) 제거 → 클라이언트가 렌더링한 코스 이미지가 대신한다
--   · 출발/도착 좌표 4개 컬럼 추가 (start_lat, start_lng, end_lat, end_lng)
--   · image_url 필수(NOT NULL) — 경로를 좌표로 안 남기므로 이미지가 없으면 기록의 의미가 없다
--   · started_at 선택(NULL 허용) — 클라이언트가 주면 얼리버드 업적 판정에 쓰고, 없으면 그만
--   · finished_at = 기록의 날짜. 클라이언트가 주면 그 값, 없으면 저장 시각
--   · client_run_id 추가 + UNIQUE(user_id, client_run_id) — 재전송 중복 저장 방지(멱등키)
--
-- 왜 수동 SQL 인가:
--   ddl-auto=update 는 컬럼 "추가"만 한다. 기존 컬럼 삭제·타입/NULL 여부 변경은 하지 않는다.
--
-- 기존 기록은 전부 삭제하기로 했으므로(좌표가 없어 이관 불가) 테이블을 통째로 버리고
-- 애플리케이션이 엔티티대로 새로 만들게 한다 — 스키마 불일치 여지가 없는 가장 확실한 방법.
--
-- 적용 시점: 배포 "직전". 이 SQL 실행 후 새 코드가 뜨면서 테이블을 다시 만든다.
--            (구버전 코드가 살아있는 동안에는 러닝 저장이 실패하므로 배포와 붙여서 실행할 것)

DROP TABLE IF EXISTS run;

-- ⚠️ 업로드된 기록 이미지는 파일시스템에 남는다(고아 파일).
--    테이블만 지우면 /var/lib/dallyeo/uploads/runs/ 의 파일은 그대로이므로 같이 정리해야 한다:
--      sudo rm -f /var/lib/dallyeo/uploads/runs/*
--    (기록을 전부 버리기로 했으므로 파일도 남길 이유가 없다. 경로는 UPLOAD_DIR 설정 확인)
--
-- 참고: user_achievement 는 건드리지 않는다. 러닝을 지워도 이미 달성한 업적은 남는다.
--       테스트 계정을 초기화하려면 아래 주석을 풀 것.
-- DELETE FROM user_achievement;

-- 확인용 (앱 기동 후)
-- SHOW CREATE TABLE run;
