package org.dpbe.global.config;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.dpbe.global.auth.dto.AuthenticatedUser;
import org.dpbe.global.auth.service.AuthService;
import org.dpbe.global.exception.ErrorResponse;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import tools.jackson.databind.ObjectMapper;

@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final ObjectMapper objectMapper;

    public AuthInterceptor(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws IOException {
        if (HttpMethod.OPTIONS.matches(request.getMethod())) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null) {
            writeError(response, request, HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "로그인이 필요합니다.");
            return false;
        }

        AuthenticatedUser user = AuthService.getAuthenticatedUser(session);
        if (user.passwordChangeRequired() && !request.getRequestURI().startsWith("/api/auth/")) {
            writeError(response, request, HttpStatus.FORBIDDEN, "FORBIDDEN", "비밀번호 변경이 필요합니다.");
            return false;
        }
        return true;
    }

    private void writeError(
            HttpServletResponse response,
            HttpServletRequest request,
            HttpStatus status,
            String code,
            String message
    ) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        ErrorResponse errorResponse = ErrorResponse.of(status, code, message, request.getRequestURI());
        objectMapper.writeValue(response.getWriter(), errorResponse);
    }
}
