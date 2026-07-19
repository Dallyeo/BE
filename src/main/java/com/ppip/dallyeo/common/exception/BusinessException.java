package com.ppip.dallyeo.common.exception;

/**
 * 도메인/비즈니스 규칙 위반 예외. {@link ErrorCode}로 HTTP status·코드가 결정된다.
 * 각 도메인 유닛은 이 예외를 그대로 쓰거나 하위 클래스로 특화할 수 있다.
 */
public class BusinessException extends RuntimeException {

    private final ErrorCode errorCode;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.defaultMessage());
        this.errorCode = errorCode;
    }

    public BusinessException(ErrorCode errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
    }

    public ErrorCode getErrorCode() {
        return errorCode;
    }
}
