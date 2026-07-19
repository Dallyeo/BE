package com.ppip.dallyeo.common.exception;

/**
 * 외부 API(TourAPI) 연동 실패 예외 (BR-7). 항상 {@link ErrorCode#EXTERNAL_API_ERROR}(502)로 매핑된다.
 * 서킷 오픈/재시도 소진/타임아웃/HTTP 오류를 사용자에게 노출하지 않고 이 예외로 정규화한다.
 */
public class ExternalApiException extends BusinessException {

    public ExternalApiException(String message, Throwable cause) {
        super(ErrorCode.EXTERNAL_API_ERROR, message);
        if (cause != null) {
            initCause(cause);
        }
    }

    public ExternalApiException(String message) {
        this(message, null);
    }
}
