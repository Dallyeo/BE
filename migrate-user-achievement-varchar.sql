-- 업적 카탈로그 8종 → 21종 확장에 따른 마이그레이션 (2026-09-13)
--
-- 왜 필요한가:
--   user_achievement.achievement 는 Hibernate MySQL 방언이 만든 네이티브 enum 컬럼이라
--   옛 8종 값만 허용한다. ddl-auto=update 는 기존 컬럼 정의를 바꾸지 않으므로,
--   코드만 배포하면 신규 업적 저장이 "Data truncated for column 'achievement'" 로 실패한다.
--
-- 이후로는 VARCHAR라 업적이 더 늘어도 이 마이그레이션을 다시 할 필요가 없다.
--
-- 적용 시점: 배포 "전"에 실행해도 되고(기존 8종은 문자열로 그대로 유효) 무중단이다.

ALTER TABLE user_achievement
    MODIFY COLUMN achievement VARCHAR(40) NOT NULL;

-- 확인용
-- SHOW CREATE TABLE user_achievement;
