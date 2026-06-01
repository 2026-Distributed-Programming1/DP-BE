package org.dpbe.global.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.dpbe.global.auth.entity.UserRole;

public record StaffAccountCreateRequest(
        @NotBlank(message = "로그인 아이디를 입력하세요.")
        @Size(min = 4, max = 50, message = "로그인 아이디는 4자 이상 50자 이하로 입력하세요.")
        String username,

        @NotBlank(message = "직원 표시 이름을 입력하세요.")
        @Size(max = 100, message = "직원 표시 이름은 100자 이하로 입력하세요.")
        String displayName,

        @NotNull(message = "직원 역할을 입력하세요.")
        UserRole role
) {
}
