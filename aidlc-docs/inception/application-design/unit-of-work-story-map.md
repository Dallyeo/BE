# Unit of Work ↔ Story Map — Dallyeo

> 21개 스토리를 유닛에 배정. U1은 U1-a/U1-b 분리 반영. (모든 스토리 배정 확인 완료)

---

## 유닛별 스토리

### U1-a — 공개 기반 (지금)
| 스토리 | 비고 |
|---|---|
| US-COMMON-1 공통 응답 래퍼 | |
| US-COMMON-2 전역 예외 처리 | |
| US-COMMON-4 TourAPI 연동 클라이언트 | Facade + Resilience4j + Redis 캐시 |
| US-COMMON-3 설정 정리 (부분) | **로거 교정**은 U1-a / JWT 라이브러리 추가는 U1-b |
| (US-AUTH-4 부분) | Security **permitAll 껍데기**만 U1-a |

### U1-b — 인증 기반 (U4 착수 시)
| 스토리 | 비고 |
|---|---|
| US-AUTH-4 인증 미들웨어/보호 엔드포인트 | deny-by-default + 화이트리스트 정교화, JWT 필터 |
| US-COMMON-3 설정 정리 (부분) | **JWT 라이브러리 추가** |

### U2 — 지역·코스조회 🌐
| 스토리 |
|---|
| US-REGION-1 지역 목록 조회 |
| US-COURSE-1 지역별 공식 코스 목록 조회 |
| US-COURSE-2 코스 상세 조회 |

### U3 — 장소·배지 🌐
| 스토리 |
|---|
| US-PLACE-1 키워드 장소 검색 |
| US-PLACE-2 지역/현위치 기반 장소 목록 |
| US-PLACE-3 주변 장소 조회(반경) |
| US-PLACE-4 장소 상세 조회 |
| US-BADGE-1 모범음식점/착한가격업소 배지 |

### U4 — 인증·사용자 🔒 (U1-b 포함)
| 스토리 |
|---|
| US-AUTH-1 소셜 로그인 |
| US-AUTH-2 토큰 발급 및 만료 정책 |
| US-AUTH-3 토큰 갱신 |
| US-AUTH-5 로그아웃 |
| US-USER-1 온보딩 신체정보 저장 |
| US-USER-2 프로필 조회 및 수정 |
| US-USER-3 계정 삭제 |

### U5 — 러닝기록·코스생성 🔒
| 스토리 |
|---|
| US-RUN-1 완료된 러닝 기록 저장 |
| US-RUN-2 러닝 기록 목록 조회 |
| US-RUN-3 러닝 기록 상세 조회 |
| US-COURSE-3 사용자 코스 직접 생성 |

---

## 커버리지 검증
- **전체 스토리 21개** 전부 배정됨. ✅
- 분할 스토리: **US-AUTH-4**(permitAll→U1-a / deny-by-default→U1-b), **US-COMMON-3**(로거→U1-a / JWT 라이브러리→U1-b).
- 인증(🔒) 스토리는 전부 U4/U5 + U1-b에 집중 → 공개 유닛(U2/U3)은 인증 무관.

## 보류 백로그(유닛 미배정 — 회의/후순위)
완주율·통계·공유결과 · 유사/최근검색어 · 프로필사진 · 약관 · 업적 · 편의시설(화장실) 좌표.
