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

    @Test
    void list_delegates() {
        List<AchievementResponse> list = List.of(
                new AchievementResponse("JJAMPPONG", "짬뽕을 먹을 자격이 있는 자", "군산 짬뽕거리 코스를 완주한 사람", true, Instant.now()),
                new AchievementResponse("GUNSAN_CONQUEROR", "군산 런트립 정복자", "군산의 모든 추천 코스를 완주한 사람", false, null));
        when(achievementService.list(7L)).thenReturn(list);

        ApiResponse<List<AchievementResponse>> res = controller.list(7L);

        assertThat(res.success()).isTrue();
        assertThat(res.data()).hasSize(2);
    }

    @Test
    void unlock_delegates() {
        AchievementResponse r = new AchievementResponse("JJAMPPONG", "짬뽕을 먹을 자격이 있는 자", "군산 짬뽕거리 코스를 완주한 사람", true, Instant.now());
        when(achievementService.unlock(7L, "JJAMPPONG")).thenReturn(r);

        ApiResponse<AchievementResponse> res = controller.unlock(7L, "JJAMPPONG");

        assertThat(res.data().code()).isEqualTo("JJAMPPONG");
        verify(achievementService).unlock(7L, "JJAMPPONG");
    }
}
