-- 코스 대표 이미지 백필 (2026-09-15)
--
-- 왜 필요한가:
--   시드 로더(CourseDataLoader)는 이미 존재하는 코스 행을 갱신하지 않는다(최초 1회 삽입만).
--   courses.json 에 imageUrl 을 넣어도 운영 DB의 기존 행에는 반영되지 않으므로 여기서 직접 채운다.
--
-- 파일은 classpath static 이라 JAR 안에 포함된다 — 서버에 따로 업로드할 필요 없다.
-- 배포 후 실행하면 되고, 순서에 민감하지 않다(이미지가 없으면 해당 필드만 응답에서 빠질 뿐).
--
-- 시드 코스 10개 전부 포함한다(2026-09-15 근대 역사 박물관 이미지 수급 완료).

UPDATE course SET image_url = '/images/courses/gunsan-modern-history-run.png'   WHERE id = 'gunsan-modern-history-run';
UPDATE course SET image_url = '/images/courses/gunsan-jjamppong-run.png'        WHERE id = 'gunsan-jjamppong-run';
UPDATE course SET image_url = '/images/courses/gunsan-seonyudo-beach-run.png'   WHERE id = 'gunsan-seonyudo-beach-run';
UPDATE course SET image_url = '/images/courses/gunsan-saemangeum-run.png'       WHERE id = 'gunsan-saemangeum-run';
UPDATE course SET image_url = '/images/courses/gunsan-wetland-eco-run.png'      WHERE id = 'gunsan-wetland-eco-run';
UPDATE course SET image_url = '/images/courses/gunsan-cypress-forest-run.png'   WHERE id = 'gunsan-cypress-forest-run';
UPDATE course SET image_url = '/images/courses/jeonju-hanok-village-run.png'    WHERE id = 'jeonju-hanok-village-run';
UPDATE course SET image_url = '/images/courses/jeonju-ajung-lake-run.png'       WHERE id = 'jeonju-ajung-lake-run';
UPDATE course SET image_url = '/images/courses/jeonju-zoo-run.png'              WHERE id = 'jeonju-zoo-run';
UPDATE course SET image_url = '/images/courses/jeonju-catholic-shrine-run.png'  WHERE id = 'jeonju-catholic-shrine-run';

-- 확인용
-- SELECT id, image_url FROM course ORDER BY id;
