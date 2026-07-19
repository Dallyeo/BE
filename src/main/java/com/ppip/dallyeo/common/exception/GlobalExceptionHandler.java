package com.ppip.dallyeo.common.exception;

import com.ppip.dallyeo.common.response.ApiError;
import com.ppip.dallyeo.common.response.ApiResponse;
import com.ppip.dallyeo.common.response.ErrorDetail;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;

/**
 * 전역 예외 처리 (US-COMMON-2, business-logic-model §2).
 * 모든 실패를 공통 실패 응답 {@link ApiResponse#failure}로 정규화한다.
 * 내부 스택/외부 원문은 응답에 노출하지 않는다(보안). 상세는 로그로만 남긴다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    /** Bean Validation 실패 → 400, VALIDATION_ERROR + 필드별 details (BR-2). */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidation(MethodArgumentNotValidException ex) {
        List<ErrorDetail> details = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ErrorDetail(fe.getField(), fe.getDefaultMessage()))
                .toList();
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), details);
    }

    /** 잘못된 요청(도메인 IllegalArgument) → 400, BAD_REQUEST. */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiResponse<Void>> handleIllegalArgument(IllegalArgumentException ex) {
        return build(ErrorCode.BAD_REQUEST, ex.getMessage(), null);
    }

    /** 필수 파라미터 누락 → 400, VALIDATION_ERROR. */
    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingParam(MissingServletRequestParameterException ex) {
        List<ErrorDetail> details = List.of(new ErrorDetail(ex.getParameterName(), "필수 파라미터가 누락되었습니다."));
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), details);
    }

    /** 파라미터 타입 불일치(예: 숫자 자리에 문자) → 400, VALIDATION_ERROR. */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiResponse<Void>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        List<ErrorDetail> details = List.of(new ErrorDetail(ex.getName(), "값의 형식이 올바르지 않습니다."));
        return build(ErrorCode.VALIDATION_ERROR, ErrorCode.VALIDATION_ERROR.defaultMessage(), details);
    }

    /** 권한 없음 → 403, FORBIDDEN. */
    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiResponse<Void>> handleAccessDenied(AccessDeniedException ex) {
        return build(ErrorCode.FORBIDDEN, ErrorCode.FORBIDDEN.defaultMessage(), null);
    }

    /** 외부 TourAPI 오류/서킷오픈/타임아웃 → 502, EXTERNAL_API_ERROR. */
    @ExceptionHandler(ExternalApiException.class)
    public ResponseEntity<ApiResponse<Void>> handleExternal(ExternalApiException ex) {
        log.warn("External API failure: {}", ex.getMessage());
        return build(ex.getErrorCode(), ex.getMessage(), null);
    }

    /** 도메인/비즈니스 예외 → ErrorCode에 따른 status/code. */
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusiness(BusinessException ex) {
        return build(ex.getErrorCode(), ex.getMessage(), null);
    }

    /** 미분류 서버 오류 → 500, INTERNAL_ERROR (원문 미노출). */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpected(Exception ex) {
        log.error("Unhandled exception", ex);
        return build(ErrorCode.INTERNAL_ERROR, ErrorCode.INTERNAL_ERROR.defaultMessage(), null);
    }

    private ResponseEntity<ApiResponse<Void>> build(ErrorCode code, String message, List<ErrorDetail> details) {
        String msg = (message == null || message.isBlank()) ? code.defaultMessage() : message;
        ApiError error = ApiError.of(code.code(), msg, details);
        return ResponseEntity.status(code.status()).body(ApiResponse.failure(error));
    }
}
