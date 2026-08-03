package com.ppip.dallyeo.user;

import org.springframework.stereotype.Component;

import java.util.concurrent.ThreadLocalRandom;

/**
 * 닉네임 결정 (US-AUTH-1, BR-5.2/5.3, FD Q4=A).
 * 소셜 프로필 닉네임이 있으면 사용, 없으면(Apple 등) 자동생성 "러너"+랜덤 4자리.
 * 유니크 제약 없음(중복 허용). 사용자는 이후 PATCH로 변경 가능.
 */
@Component
public class NicknameGenerator {

    private static final String PREFIX = "러너";

    /** 소셜 닉네임 우선, 공백/null이면 자동생성. */
    public String resolve(String socialNickname) {
        if (socialNickname != null && !socialNickname.isBlank()) {
            return socialNickname.trim();
        }
        return generate();
    }

    /** "러너" + 1000~9999 랜덤. */
    public String generate() {
        int suffix = ThreadLocalRandom.current().nextInt(1000, 10000);
        return PREFIX + suffix;
    }
}
