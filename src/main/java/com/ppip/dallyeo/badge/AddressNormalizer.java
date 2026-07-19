package com.ppip.dallyeo.badge;

import org.springframework.stereotype.Component;

/**
 * 업소명/주소 정규화 (BR-U3-9, D1=A 보수적).
 * 공백/괄호(내용 포함)/특수문자 제거, 주소는 시도명 표기 통일. 그 이상(도로명 파싱)은 하지 않음(오매칭 방지).
 */
@Component
public class AddressNormalizer {

    /** 업소명 정규화: 괄호 제거 → 비문자/숫자 제거 → 소문자. */
    public String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return stripToAlphaNum(removeParentheses(name)).toLowerCase();
    }

    /** 주소 정규화: 시도명 통일 → 괄호 제거 → 비문자/숫자 제거 → 소문자. */
    public String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        String unified = unifyProvince(address);
        return stripToAlphaNum(removeParentheses(unified)).toLowerCase();
    }

    /** 전북특별자치도 = 전라북도 = 전북 → "전북"으로 통일. */
    private String unifyProvince(String s) {
        return s.replace("전북특별자치도", "전북").replace("전라북도", "전북");
    }

    private String removeParentheses(String s) {
        // 반각/전각 소괄호 + 대괄호와 그 안 내용 제거.
        // 대괄호: TourAPI가 상호에 붙이는 태그(예: "[착한가게] 아서원", "[모범음식점]") 제거 목적.
        return s.replaceAll("\\(.*?\\)", "")
                .replaceAll("（.*?）", "")
                .replaceAll("\\[.*?\\]", "");
    }

    /** 문자(모든 언어)·숫자 외 전부 제거(공백/특수문자/구두점 포함). */
    private String stripToAlphaNum(String s) {
        return s.replaceAll("[^\\p{L}\\p{N}]", "");
    }
}
