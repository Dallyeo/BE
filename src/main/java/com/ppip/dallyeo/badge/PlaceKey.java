package com.ppip.dallyeo.badge;

/**
 * 배지 일괄 조회 입력 키 (원본 업소명 + 주소).
 *
 * <p>호출자는 정규화 규칙을 알 필요가 없다 — 정규화는 {@link BadgeService} 내부에서 하고,
 * 결과 맵은 여기 넣은 원본 값 그대로를 키로 돌려준다.
 */
public record PlaceKey(String name, String address) {
}
