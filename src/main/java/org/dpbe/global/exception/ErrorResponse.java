package org.dpbe.global.exception;

import java.time.LocalDateTime;

/** 표준 에러 응답 DTO */
public record ErrorResponse(
        int status,
        String error,
        String message,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, message, LocalDateTime.now());
    }
}