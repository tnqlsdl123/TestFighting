package com.test.fighting.domain.user;

import com.test.fighting.domain.user.dto.request.SignupRequest;
import com.test.fighting.domain.user.entity.User;
import com.test.fighting.domain.user.repository.UserRepository;
import com.test.fighting.domain.user.service.UserService;
import com.test.fighting.global.code.GlobalErrorCode;
import com.test.fighting.global.exception.GeneralException;
import org.hibernate.exception.ConstraintViolationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class UserServiceTest {
    private final UserRepository repository = mock(UserRepository.class);
    private final PasswordEncoder encoder = mock(PasswordEncoder.class);
    private final UserService service = new UserService(repository, encoder);
    private final SignupRequest request = new SignupRequest("test1234", "a1", "수빈");

    @ParameterizedTest
    @CsvSource({
            "uk_users_username,DUPLICATE_USER_ID",
            "users.uk_users_username,DUPLICATE_USER_ID",
            "uk_users_nickname,DUPLICATE_NICKNAME"
    })
    void databaseDuplicateAfterPrecheckMapsTo409(String constraint, GlobalErrorCode expected) {
        var violation = mock(ConstraintViolationException.class);
        when(violation.getConstraintName()).thenReturn(constraint);
        when(encoder.encode("a1")).thenReturn("hashed-password");
        when(repository.saveAndFlush(any(User.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate", violation));

        var exception = assertThrows(GeneralException.class, () -> service.signup(request));
        assertThat(exception.getErrorCode()).isEqualTo(expected);
    }

    @Test
    void unrelatedDatabaseFailureIsNotReportedAsDuplicate() {
        var failure = new DataIntegrityViolationException("other database constraint");
        when(encoder.encode("a1")).thenReturn("hashed-password");
        when(repository.saveAndFlush(any(User.class))).thenThrow(failure);

        assertThatThrownBy(() -> service.signup(request)).isSameAs(failure);
    }
}
