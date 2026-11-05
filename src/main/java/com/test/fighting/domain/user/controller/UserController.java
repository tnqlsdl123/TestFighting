package com.test.fighting.domain.user.controller;

import com.test.fighting.domain.user.dto.request.SignupRequest;
import com.test.fighting.domain.user.dto.response.SignupResponse;
import com.test.fighting.domain.user.service.UserService;
import com.test.fighting.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/users")
public class UserController {
    private final UserService userService;

    @PostMapping("/signup")
    public ApiResponse<SignupResponse> signup(@Valid @RequestBody SignupRequest request) {
        return ApiResponse.success("회원가입에 성공했습니다.", userService.signup(request));
    }
}
