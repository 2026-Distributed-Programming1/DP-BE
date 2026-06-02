package org.dpbe.global.exception;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

/** 표준 에러 응답 DTO */
public record ErrorResponse(
        int status,
        String error,
        String code,
        String message,
        String path,
        List<FieldError> fieldErrors,
        LocalDateTime timestamp
) {
    public static ErrorResponse of(int status, String error, String message) {
        return new ErrorResponse(status, error, null, message, null, List.of(), LocalDateTime.now());
    }

    public static ErrorResponse of(HttpStatus status, String code, String message, String path) {
        return new ErrorResponse(
                status.value(),
                status.getReasonPhrase(),
                code,
                message,
                path,
                List.of(),
                LocalDateTime.now());
    }

    public static ErrorResponse validation(String path, List<FieldError> fieldErrors) {
        return new ErrorResponse(
                HttpStatus.BAD_REQUEST.value(),
                HttpStatus.BAD_REQUEST.getReasonPhrase(),
                "VALIDATION_ERROR",
                "요청값 검증에 실패했습니다.",
                path,
                fieldErrors,
                LocalDateTime.now());
    }

    public record FieldError(
            String field,
            String message
    ) {
    }
}
