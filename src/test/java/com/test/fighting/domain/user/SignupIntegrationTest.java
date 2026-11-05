package com.test.fighting.domain.user;

import com.jayway.jsonpath.JsonPath;
import com.test.fighting.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SignupIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired UserRepository repository;
    @Autowired PasswordEncoder passwordEncoder;

    private MockHttpSession session;
    private String csrfToken;
    private String csrfHeader;

    @BeforeEach
    void setUp() throws Exception {
        repository.deleteAll();
        var result = mvc.perform(get("/api/csrf"))
                .andExpect(status().isOk()).andReturn();
        session = (MockHttpSession) result.getRequest().getSession(false);
        String body = result.getResponse().getContentAsString();
        csrfToken = JsonPath.read(body, "$.result.token");
        csrfHeader = JsonPath.read(body, "$.result.headerName");
    }

    @Test
    void signupTrimsFieldsAndStoresOnlyPasswordHash() throws Exception {
        signup("""
                {"username":" test1234 ","password":" password123! ","nickname":" 수 빈 "}
                """)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.isSuccess").value(true))
                .andExpect(jsonPath("$.code").value("COMMON_200"))
                .andExpect(jsonPath("$.message").value("회원가입에 성공했습니다."))
                .andExpect(jsonPath("$.result.id").isNumber())
                .andExpect(jsonPath("$.result.username").value("test1234"))
                .andExpect(jsonPath("$.result.nickname").value("수 빈"))
                .andExpect(jsonPath("$.result.password").doesNotExist())
                .andExpect(jsonPath("$.result.passwordHash").doesNotExist());

        var user = repository.findAll().get(0);
        assertThat(user.getPasswordHash()).isNotEqualTo("password123!");
        assertThat(passwordEncoder.matches("password123!", user.getPasswordHash())).isTrue();
    }

    @Test
    void usernameComparisonIsCaseSensitive() throws Exception {
        signup("""
                {"username":"test1234","password":"a1","nickname":"수빈"}
                """).andExpect(status().isOk());
        signup("""
                {"username":"Test1234","password":"b2","nickname":"다빈"}
                """).andExpect(status().isOk());
        assertThat(repository.count()).isEqualTo(2);
    }

    @Test
    void duplicateUsernameReturns409() throws Exception {
        signup("""
                {"username":"test1234","password":"a1","nickname":"수빈"}
                """).andExpect(status().isOk());
        signup("""
                {"username":" test1234 ","password":"b2","nickname":"다빈"}
                """).andExpect(status().isConflict())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("USER_4091"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 아이디입니다."))
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()));
    }

    @Test
    void duplicateNicknameReturns409() throws Exception {
        signup("""
                {"username":"test1234","password":"a1","nickname":"수빈"}
                """).andExpect(status().isOk());
        signup("""
                {"username":"other123","password":"b2","nickname":" 수빈 "}
                """).andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("USER_4092"))
                .andExpect(jsonPath("$.message").value("이미 사용 중인 닉네임입니다."));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "{}",
            "{\"username\":\"x\",\"password\":\"a1\",\"nickname\":\"수빈\"}",
            "{\"username\":\"12345678901\",\"password\":\"a1\",\"nickname\":\"수빈\"}",
            "{\"username\":\"valid\",\"password\":\"abcd\",\"nickname\":\"수빈\"}",
            "{\"username\":\"valid\",\"password\":\"1234\",\"nickname\":\"수빈\"}",
            "{\"username\":\"valid\",\"password\":\"a 1\",\"nickname\":\"수빈\"}",
            "{\"username\":\"valid\",\"password\":\"a1\",\"nickname\":\"수\"}",
            "{\"username\":\"valid\",\"password\":\"a1\",\"nickname\":\"수빈!\"}",
            "{\"username\":\"valid\",\"password\":\"a1\",\"nickname\":\"   \"}",
            "{broken"
    })
    void invalidRequestReturns400(String body) throws Exception {
        signup(body).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.code").value("COMMON_400"))
                .andExpect(jsonPath("$.result").value(org.hamcrest.Matchers.nullValue()));
        assertThat(repository.count()).isZero();
    }

    @Test
    void returnsFieldSpecificValidationMessage() throws Exception {
        signup("""
                {"username":"x","password":"a1","nickname":"수빈"}
                """).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("아이디는 2자 이상 10자 이하여야 합니다."));
    }

    @Test
    void signupRequiresCsrfTokenEvenWithoutAuthentication() throws Exception {
        mvc.perform(post("/api/users/signup").session(session)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"valid\",\"password\":\"a1\",\"nickname\":\"수빈\"}"))
                .andExpect(status().isForbidden());
        assertThat(repository.count()).isZero();
    }

    private ResultActions signup(String body) throws Exception {
        return mvc.perform(post("/api/users/signup")
                .session(session).header(csrfHeader, csrfToken)
                .contentType(MediaType.APPLICATION_JSON).content(body));
    }
}
