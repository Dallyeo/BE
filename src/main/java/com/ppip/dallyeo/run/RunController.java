package com.ppip.dallyeo.run;

import com.ppip.dallyeo.auth.AuthUser;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.run.dto.RunCreateRequest;
import com.ppip.dallyeo.run.dto.RunDetailResponse;
import com.ppip.dallyeo.run.dto.RunSummaryResponse;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 러닝 기록 API (US-RUN-1/2/3). 전부 보호(🔒) — 본인(@AuthUser)만.
 * deny-by-default(U1-b)로 /runs/** 는 자동 authenticated.
 */
@RestController
@RequestMapping("/runs")
public class RunController {

    private final RunService runService;

    public RunController(RunService runService) {
        this.runService = runService;
    }

    /** 러닝 기록 저장 (US-RUN-1). 검증 실패 → 400. */
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<RunDetailResponse> save(@AuthUser Long userId,
                                               @Valid @RequestBody RunCreateRequest request) {
        return ApiResponse.success(runService.save(userId, request));
    }

    /**
     * 기록 이미지 업로드/교체. multipart/form-data, 파트명 {@code image}.
     * 저장 후 갱신된 상세(imageUrl 포함)를 돌려준다. 타인/미존재 → 404.
     */
    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<RunDetailResponse> uploadImage(@AuthUser Long userId,
                                                      @PathVariable Long id,
                                                      @RequestPart("image") MultipartFile image) {
        return ApiResponse.success(runService.attachImage(userId, id, image));
    }

    /** 러닝 기록 목록 (US-RUN-2). 본인만, 기간(from/to ISO date) 선택. */
    @GetMapping
    public ApiResponse<List<RunSummaryResponse>> list(@AuthUser Long userId,
                                                      @RequestParam(required = false) String from,
                                                      @RequestParam(required = false) String to) {
        return ApiResponse.success(runService.list(userId, from, to));
    }

    /** 러닝 기록 상세 (US-RUN-3). 타인/미존재 → 404. */
    @GetMapping("/{id}")
    public ApiResponse<RunDetailResponse> detail(@AuthUser Long userId, @PathVariable Long id) {
        return ApiResponse.success(runService.getDetail(userId, id));
    }
}
