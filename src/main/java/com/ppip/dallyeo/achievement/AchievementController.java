package com.ppip.dallyeo.achievement;

import com.ppip.dallyeo.achievement.dto.AchievementResponse;
import com.ppip.dallyeo.auth.AuthUser;
import com.ppip.dallyeo.common.response.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 업적 API (US-ACHV). 전부 보호(🔒) — 본인(@AuthUser) 기준.
 * 목록은 전체 8종 + 본인 달성여부, unlock은 특정 업적 수동 달성(조건 재판정).
 * 러닝 저장 시 자동 판정은 RunService → AchievementService.evaluateAndUnlock.
 */
@RestController
@RequestMapping("/achievements")
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    /** 전체 업적 목록 + 본인 달성 여부/일시. */
    @GetMapping
    public ApiResponse<List<AchievementResponse>> list(@AuthUser Long userId) {
        return ApiResponse.success(achievementService.list(userId));
    }

    /** 특정 업적 수동 달성. 미지원 코드 → 404, 조건 미충족 → 409. */
    @PostMapping("/{code}/unlock")
    public ApiResponse<AchievementResponse> unlock(@AuthUser Long userId, @PathVariable String code) {
        return ApiResponse.success(achievementService.unlock(userId, code));
    }
}
