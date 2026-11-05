package com.test.fighting.domain.user;

import com.test.fighting.domain.user.controller.UserController;
import com.test.fighting.domain.user.dto.request.SignupRequest;
import com.test.fighting.domain.user.service.UserService;
import com.test.fighting.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class UserControllerErrorTest {
    @Test
    void unexpectedServiceFailureReturnsGlobal500Response() throws Exception {
        UserService service = mock(UserService.class);
        when(service.signup(any(SignupRequest.class))).thenThrow(new IllegalStateException("database unavailable"));
        var mvc = MockMvcBuilders.standaloneSetup(new UserController(service))
                .setControllerAdvice(new GlobalExceptionHandler()).build();

        mvc.perform(post("/api/users/signup").contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"username":"test1234","password":"a1","nickname":"수빈"}
                                """))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_500"))
                .andExpect(jsonPath("$.message").value("서버 에러가 발생했습니다."))
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()));
    }
}
