package com.test.fighting.domain.user.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "users", uniqueConstraints = {
        @UniqueConstraint(name = "uk_users_username", columnNames = "username"),
        @UniqueConstraint(name = "uk_users_nickname", columnNames = "nickname")
})
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // MySQL 기본 collation에 의한 대소문자 무시를 방지한다.
    @Column(nullable = false, length = 10,
            columnDefinition = "varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin")
    private String username;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(nullable = false, length = 255,
            columnDefinition = "varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_bin")
    private String nickname;

    public User(String username, String passwordHash, String nickname) {
        this.username = username;
        this.passwordHash = passwordHash;
        this.nickname = nickname;
    }
}
