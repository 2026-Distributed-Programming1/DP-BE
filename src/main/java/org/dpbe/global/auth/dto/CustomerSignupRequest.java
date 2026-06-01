package org.dpbe.global.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public record CustomerSignupRequest(
        @NotBlank(message = "로그인 아이디를 입력하세요.")
        @Size(min = 4, max = 50, message = "로그인 아이디는 4자 이상 50자 이하로 입력하세요.")
        String username,

        @NotBlank(message = "비밀번호를 입력하세요.")
        @Size(min = 8, max = 100, message = "비밀번호는 8자 이상 100자 이하로 입력하세요.")
        String password,

        @NotBlank(message = "이름을 입력하세요.")
        @Size(max = 100, message = "이름은 100자 이하로 입력하세요.")
        String name,

        @NotBlank(message = "주민등록번호를 입력하세요.")
        @Pattern(regexp = "\\d{6}-?\\d{7}", message = "주민등록번호 형식이 올바르지 않습니다.")
        String residentNo,

        @NotBlank(message = "연락처를 입력하세요.")
        @Pattern(regexp = "010-?\\d{4}-?\\d{4}", message = "연락처 형식이 올바르지 않습니다.")
        String phone,

        @Email(message = "이메일 형식이 올바르지 않습니다.")
        @Size(max = 100, message = "이메일은 100자 이하로 입력하세요.")
        String email,

        @NotBlank(message = "주소를 입력하세요.")
        @Size(max = 200, message = "주소는 200자 이하로 입력하세요.")
        String address,

        @NotNull(message = "생년월일을 입력하세요.")
        @Past(message = "생년월일은 과거 날짜여야 합니다.")
        LocalDate birthDate
) {
}
