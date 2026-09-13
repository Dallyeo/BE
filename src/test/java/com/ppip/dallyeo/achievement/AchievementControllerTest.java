package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.common.response.ApiResponse;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AchievementControllerTest {

    private final AchievementService achievementService = mock(AchievementService.class);
    private final AchievementController controller = new AchievementController(achievementService);

    private AchievementResponse response(String code, boolean unlocked) {
        return new AchievementResponse(code, "GUNSAN", 30, "이름", "설명",
                "/images/achievements/" + code.toLowerCase() + "_on.webp",
                "/images/achievements/" + code.toLowerCase() + "_off.webp",
                unlocked, unlocked ? Instant.now() : null);
    }

    @Test
    void list_delegates() {
        List<AchievementResponse> list = List.of(
                response("JJAMPPONG", true),
                response("GUNSAN_CONQUEROR", false));
        when(achievementService.list(7L)).thenReturn(list);

        ApiResponse<List<AchievementResponse>> res = controller.list(7L);

        assertThat(res.success()).isTrue();
        assertThat(res.data()).hasSize(2);
    }

    @Test
    void unlock_delegates() {
        AchievementResponse r = response("JJAMPPONG", true);
        when(achievementService.unlock(7L, "JJAMPPONG")).thenReturn(r);

        ApiResponse<AchievementResponse> res = controller.unlock(7L, "JJAMPPONG");

        assertThat(res.data().code()).isEqualTo("JJAMPPONG");
        verify(achievementService).unlock(7L, "JJAMPPONG");
    }
}
