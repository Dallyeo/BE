package com.ppip.dallyeo.user;

import com.ppip.dallyeo.auth.AuthUser;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.user.dto.UpdateProfileRequest;
import com.ppip.dallyeo.user.dto.UserProfileResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 사용자 프로필 API (US-USER-1/2/3). 전부 보호(🔒) — 본인(@AuthUser)만.
 */
@RestController
@RequestMapping("/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    /** 내 프로필 조회 (US-USER-2). */
    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile(@AuthUser Long userId) {
        return ApiResponse.success(userService.getMyProfile(userId));
    }

    /** 프로필/온보딩 수정 (US-USER-1/2). 부분 갱신 + 온보딩 완료. */
    @PatchMapping("/me")
    public ApiResponse<UserProfileResponse> updateMyProfile(@AuthUser Long userId,
                                                            @Valid @RequestBody UpdateProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    /** 계정 삭제 (US-USER-3). 하드 삭제 후 204. */
    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMyAccount(@AuthUser Long userId) {
        userService.deleteAccount(userId);
    }
}
