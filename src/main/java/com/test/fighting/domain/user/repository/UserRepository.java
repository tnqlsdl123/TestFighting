package com.test.fighting.domain.user.repository;

import com.test.fighting.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByUsername(String username);

    boolean existsByNickname(String nickname);
}
