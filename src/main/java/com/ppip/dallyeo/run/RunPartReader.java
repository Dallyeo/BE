package com.ppip.dallyeo.run;

import com.ppip.dallyeo.common.exception.BusinessException;
import com.ppip.dallyeo.common.exception.ErrorCode;
import com.ppip.dallyeo.run.dto.RunCreateRequest;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.Set;
import java.util.stream.Collectors;

/**
 * multipart 의 {@code run} 파트(JSON 문자열) → {@link RunCreateRequest} 변환 + 검증.
 *
 * <p>{@code @RequestPart} 로 바로 객체를 받으면 <b>파트에 {@code Content-Type: application/json}
 * 이 있어야만</b> 동작한다. iOS/안드로이드 기본 multipart 구현은 파트별 Content-Type 을 자주 생략하고,
 * 그러면 정상 요청이 500 으로 떨어져 클라이언트 입장에서 원인을 알 수 없다.
 * 그래서 문자열로 받아 여기서 직접 파싱한다 — <b>Content-Type 유무와 무관하게</b> 동작한다.
 *
 * <p>실패는 전부 400(VALIDATION_ERROR)이다. JSON 형식 오류와 필드 검증 실패 모두
 * 어디가 잘못됐는지 메시지에 담는다.
 */
@Component
public class RunPartReader {

    private final ObjectMapper objectMapper;
    private final Validator validator;

    public RunPartReader(ObjectMapper objectMapper, Validator validator) {
        this.objectMapper = objectMapper;
        this.validator = validator;
    }

    public RunCreateRequest read(String json) {
        RunCreateRequest request = parse(json);
        Set<ConstraintViolation<RunCreateRequest>> violations = validator.validate(request);
        if (!violations.isEmpty()) {
            String detail = violations.stream()
                    .map(ConstraintViolation::getMessage)
                    .sorted()
                    .collect(Collectors.joining(" / "));
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, detail);
        }
        return request;
    }

    private RunCreateRequest parse(String json) {
        if (json == null || json.isBlank()) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR, "run 파트가 비어 있습니다.");
        }
        try {
            return objectMapper.readValue(json, RunCreateRequest.class);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.VALIDATION_ERROR,
                    "run 파트의 JSON 형식이 올바르지 않습니다: " + e.getMessage());
        }
    }
}
