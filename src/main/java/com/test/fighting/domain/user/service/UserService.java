package com.test.fighting.domain.user.service;

import com.test.fighting.domain.user.dto.request.SignupRequest;
import com.test.fighting.domain.user.dto.response.SignupResponse;
import com.test.fighting.domain.user.entity.User;
import com.test.fighting.domain.user.repository.UserRepository;
import com.test.fighting.global.code.GlobalErrorCode;
import com.test.fighting.global.exception.GeneralException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public SignupResponse signup(SignupRequest request) {
        if (userRepository.existsByUsername(request.username())) {
            throw new GeneralException(GlobalErrorCode.DUPLICATE_USER_ID);
        }
        if (userRepository.existsByNickname(request.nickname())) {
            throw new GeneralException(GlobalErrorCode.DUPLICATE_NICKNAME);
        }

        User user = new User(request.username(), passwordEncoder.encode(request.password()), request.nickname());
        try {
            return SignupResponse.from(userRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            // 사전 중복 확인을 동시에 통과한 요청도 DB UNIQUE 제약으로 처리한다.
            for (Throwable cause = exception; cause != null; cause = cause.getCause()) {
                if (cause instanceof ConstraintViolationException violation) {
                    String constraint = violation.getConstraintName();
                    if (matches(constraint, "uk_users_username")) {
                        throw new GeneralException(GlobalErrorCode.DUPLICATE_USER_ID);
                    }
                    if (matches(constraint, "uk_users_nickname")) {
                        throw new GeneralException(GlobalErrorCode.DUPLICATE_NICKNAME);
                    }
                }
            }
            throw exception;
        }
    }

    private boolean matches(String actual, String expected) {
        return actual != null && (actual.equalsIgnoreCase(expected)
                || actual.toLowerCase(java.util.Locale.ROOT).endsWith("." + expected));
    }
}
