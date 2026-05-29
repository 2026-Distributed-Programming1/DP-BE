package org.dpbe.global.exception;

import org.springframework.http.HttpStatus;

/**
 * API 계층 비즈니스 예외.
 * 콘솔 Runner의 E1/검증 실패/조회 없음 분기를 HTTP 상태 + 메시지로 표현한다.
 */
public class ApiException extends RuntimeException {

    private final HttpStatus status;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static ApiException notFound(String message) {
        return new ApiException(HttpStatus.NOT_FOUND, message);
    }

    public static ApiException badRequest(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, message);
    }
}