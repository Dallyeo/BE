package com.ppip.dallyeo.user;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NicknameGeneratorTest {

    private final NicknameGenerator generator = new NicknameGenerator();

    @Test
    void usesSocialNicknameWhenPresent() {
        assertThat(generator.resolve("달려라")).isEqualTo("달려라");
    }

    @Test
    void trimsSocialNickname() {
        assertThat(generator.resolve("  달려라  ")).isEqualTo("달려라");
    }

    @Test
    void generatesWhenNull() {
        assertThat(generator.resolve(null)).matches("러너\\d{4}");
    }

    @Test
    void generatesWhenBlank() {
        assertThat(generator.resolve("   ")).matches("러너\\d{4}");
    }

    @Test
    void generateAlwaysMatchesPattern() {
        for (int i = 0; i < 50; i++) {
            assertThat(generator.generate()).matches("러너\\d{4}");
        }
    }
}
