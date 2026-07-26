package com.tmd.backend.domain.user;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "user", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"email", "provider"})
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String email;

    // OAuth2 유저는 null 허용
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    // OAuth2 유저만 사용 (Google/Kakao가 주는 고유 id)
    // Google의 응답 속 sub 필드, KaKao == id
    private String providerId;

    @Column(nullable = false)
    private boolean emailVerified;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private User(String email, String password, AuthProvider provider, String providerId, boolean emailVerified) {
        this.email = email;
        this.password = password;
        this.provider = provider;
        this.providerId = providerId;
        this.emailVerified = emailVerified;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 로컬 회원가입 생성 팩토리 메서드
    public static User createLocal(String email, String encodedPassword) {
        return User.builder()
            .email(email)
            .password(encodedPassword)
            .provider(AuthProvider.LOCAL)
            .emailVerified(false)
            .build();
    }

    // OAuth2 회원가입 생성 팩토리 메서드
    public static User createOAuth(String email, AuthProvider provider, String providerId) {
        return User.builder()
            .email(email)
            .provider(provider)
            .providerId(providerId)
            .emailVerified(true) // 이미 검증된 이메일이므로 바로 true
            .build();
    }

    public void verifyEmail() {
        this.emailVerified = true;
    }
}
