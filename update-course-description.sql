-- 코스 description 백필 (courses.json 기준). 실행: mysql -u root -p dallyeo < update-course-description.sql
START TRANSACTION;
UPDATE course SET description = '은파호수공원을 한 바퀴 돌아 군산근대역사박물관으로 이어지는 코스입니다. 중간에 자리한 분위기 좋은 카페에서 잠시 숨을 고르고, 박물관에서 여정을 마무리해보세요.' WHERE id = 'gunsan-modern-history-run';
UPDATE course SET description = '새만금 방조제를 달리며 탁 트인 바다를 만끽하는 코스입니다. 출발 전 비응항과 새만금 수산시장을 둘러보는 것도 좋습니다.' WHERE id = 'gunsan-saemangeum-run';
UPDATE course SET description = '군산수송공원에서 철길마을을 지나 해변가를 따라 짬뽕거리까지 달리는 코스입니다. 가볍게 몸을 푼 뒤, 군산의 명물 짬뽕을 더 맛있게 즐겨보세요.' WHERE id = 'gunsan-jjamppong-run';
UPDATE course SET description = '선유도 해변을 따라 달리는 코스입니다. 명사십리 백사장과 몽돌해변을 지나며, 고군산군도의 정취를 느긋하게 만끽해보세요.' WHERE id = 'gunsan-seonyudo-beach-run';
UPDATE course SET description = '군산 도심에서 출발해 금강호를 따라 습지생태공원까지 달리는 코스입니다. 달리기를 마친 뒤 공원을 거닐며 자연의 여유를 만끽해보세요.' WHERE id = 'gunsan-wetland-eco-run';
UPDATE course SET description = '군산 도심에서 출발해 편백치유의숲까지 이어지는 코스입니다. 한적한 길을 달린 뒤, 편백나무 사이를 거닐며 피톤치드를 마음껏 누려보세요.' WHERE id = 'gunsan-cypress-forest-run';
UPDATE course SET description = '전주 한옥마을 일대를 가볍게 한 바퀴 도는 코스입니다. 곳곳에 자리한 크고 작은 문화유산을 둘러보며 전주의 정취를 느껴보세요.' WHERE id = 'jeonju-hanok-village-run';
UPDATE course SET description = '도심에서 출발해 아중호수를 한 바퀴 도는 코스입니다. 달리기를 마친 뒤 호숫가 카페에서 잔잔한 물결을 바라보며 여유를 누려보세요.' WHERE id = 'jeonju-ajung-lake-run';
UPDATE course SET description = '덕진공원에서 출발해 전주동물원으로 이어지는 코스입니다. 가볍게 달린 뒤 동물들을 만나며 즐거운 시간을 보내보세요.' WHERE id = 'jeonju-zoo-run';
UPDATE course SET description = '전주고속버스터미널에서 출발해 전주의 천주교 순교성지를 돌아보는 코스입니다. 서천교 순교터를 지나 치명자산성지 평화의 전당에 오르며 경건한 시간을 가져보세요.' WHERE id = 'jeonju-catholic-shrine-run';
COMMIT;
