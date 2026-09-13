package com.ppip.dallyeo.badge;

import org.springframework.stereotype.Component;

/**
 * 업소명/주소 정규화 (BR-U3-9, D1=A 보수적).
 * 공백/괄호(내용 포함)/특수문자 제거, 주소는 시도명 표기 통일 + 상세주소(층/호) 절단.
 * 그 이상(도로명 파싱, 유사도 매칭)은 하지 않음(오매칭 방지).
 */
@Component
public class AddressNormalizer {

    /** 괄호로 묶인 층 표기. 예: "하나운1길 15 (1,2)층" — 일반 괄호 제거보다 먼저 걷어내야 건물번호가 안 깎인다. */
    private static final String PARENTHESIZED_FLOOR = "\\(\\s*[\\d.,\\s~-]+\\s*\\)\\s*층";

    /** 콤마 절단 후 남는 층 표기. 예: "… 15 층". */
    private static final String RESIDUAL_FLOOR = "(지하\\s*)?\\d*\\s*층";

    /** 업소명 정규화: 괄호 제거 → 비문자/숫자 제거 → 소문자. */
    public String normalizeName(String name) {
        if (name == null) {
            return "";
        }
        return stripToAlphaNum(removeParentheses(name)).toLowerCase();
    }

    /**
     * 주소 정규화: 시도명 통일 → 층 표기·괄호 제거 → <b>상세주소 절단</b> → 비문자/숫자 제거 → 소문자.
     *
     * <p>공공데이터 주소는 도로명주소 규약대로 <b>콤마 뒤가 상세주소</b>(층·호·동)인데,
     * TourAPI 주소에는 이 부분이 대개 없다. 그대로 두면 같은 가게가 표기 차이만으로 탈락한다.
     * (예: CSV "수송남로 2, 1층" vs TourAPI "수송남로 2")
     * 도로명+건물번호까지는 그대로 비교하므로 서로 다른 건물이 뭉칠 위험은 없다.
     */
    public String normalizeAddress(String address) {
        if (address == null) {
            return "";
        }
        String s = unifyProvince(address);
        s = s.replaceAll(PARENTHESIZED_FLOOR, "");   // ① "(1,2)층" — 괄호 제거보다 먼저
        s = removeParentheses(s);                    // ② 나머지 괄호(법정동 등)
        s = cutDetailAddress(s);                     // ③ 콤마 뒤 상세주소 절단
        s = s.replaceAll(RESIDUAL_FLOOR, "");        // ④ 잔여 "층"
        return stripToAlphaNum(s).toLowerCase();
    }

    /** 도로명주소의 콤마 뒤 상세주소(층/호/동)를 잘라낸다. 콤마가 없으면 그대로. */
    private String cutDetailAddress(String s) {
        int comma = s.indexOf(',');
        return comma < 0 ? s : s.substring(0, comma);
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
