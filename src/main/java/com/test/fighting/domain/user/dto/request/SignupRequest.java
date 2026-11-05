package com.test.fighting.domain.user.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SignupRequest(
        @NotBlank(message = "아이디는 필수 입력값입니다.")
        @Size(min = 2, max = 10, message = "아이디는 2자 이상 10자 이하여야 합니다.")
        String username,

        @NotBlank(message = "비밀번호는 필수 입력값입니다.")
        @Size(min = 2, message = "비밀번호는 2자 이상이어야 합니다.")
        @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*[0-9])[\\x21-\\x7E]+$",
                message = "비밀번호는 영문과 숫자를 포함해야 하며, 공백 없이 영문·숫자·특수문자를 사용할 수 있습니다.")
        String password,

        @NotBlank(message = "닉네임은 필수 입력값입니다.")
        @Size(min = 2, max = 255, message = "닉네임은 2자 이상 255자 이하여야 합니다.")
        @Pattern(regexp = "^[가-힣ㄱ-ㅎㅏ-ㅣA-Za-z0-9 ]+$",
                message = "닉네임은 한글, 영문, 숫자, 공백만 사용할 수 있습니다.")
        String nickname
) {
    public SignupRequest {
        username = strip(username);
        password = strip(password);
        nickname = strip(nickname);
    }

    private static String strip(String value) {
        return value == null ? null : value.strip();
    }
}
