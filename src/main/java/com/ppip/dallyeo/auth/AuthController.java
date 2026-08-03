package com.ppip.dallyeo.auth;

import com.ppip.dallyeo.auth.dto.LoginRequest;
import com.ppip.dallyeo.auth.dto.RefreshRequest;
import com.ppip.dallyeo.auth.dto.TokenResponse;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.user.Provider;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * 인증 API (US-AUTH-1/3/5). login/refresh는 공개(🌐), logout은 보호(🔒).
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    /** 소셜 로그인/가입 (US-AUTH-1). 미지원 provider → 400. */
    @PostMapping("/login/{provider}")
    public ApiResponse<TokenResponse> login(@PathVariable String provider,
                                            @Valid @RequestBody LoginRequest request) {
        Provider p = Provider.from(provider);
        return ApiResponse.success(authService.login(p, request.authorizationCode()));
    }

    /** 토큰 갱신 (US-AUTH-3). 무효/불일치 → 401. */
    @PostMapping("/refresh")
    public ApiResponse<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ApiResponse.success(authService.refresh(request.refreshToken()));
    }

    /** 로그아웃 (US-AUTH-5). refresh 무효화 후 204. */
    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void logout(@AuthUser Long userId) {
        authService.logout(userId);
    }
}
