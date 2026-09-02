# 코스 대표 이미지

이 디렉터리의 파일은 `/images/courses/{파일명}` 으로 공개 서빙됩니다(인증 불필요).

## 추가 방법
1. 이미지 파일을 이 디렉터리에 둡니다. 파일명은 코스 id 기준을 권장합니다.
   예) `gunsan-jjamppong-run.png`
2. `src/main/resources/data/courses.json` 의 해당 코스에 `imageUrl` 을 넣습니다.
   ```json
   "imageUrl": "/images/courses/gunsan-jjamppong-run.png"
   ```
3. 이미 시드된 운영 DB는 시드 로더가 갱신하지 않으므로(최초 1회만 삽입),
   `update-course-image.sql` 같은 백필 SQL로 `course.image_url` 을 채웁니다.

`imageUrl` 이 없으면 목록/상세 응답에서 해당 필드가 생략됩니다(NON_NULL 직렬화).
