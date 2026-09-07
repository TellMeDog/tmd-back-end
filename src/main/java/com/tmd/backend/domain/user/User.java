// User Entity 파일

package com.tmd.backend.domain.user;

import com.tmd.backend.domain.pet.Pet;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import com.tmd.backend.domain.review.Review;  // [수정] Review import 추가

@Entity
@Table(name = "\"user\"", uniqueConstraints = {
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

    @Column(nullable = false, length = 20)
    private String nickname;  // [수정] 필드 추가: 로컬 회원가입 시 사용자가 직접 입력하는 닉네임

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AuthProvider provider;

    // OAuth2 유저만 사용 (Google/Kakao가 주는 고유 id)
    // Google의 응답 속 sub 필드, KaKao == id
    private String providerId;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Pet> pets = new ArrayList<>();  // 회원 탈퇴 시 반려동물도 자동 삭제되도록 Cascade 적용

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Review> reviews = new ArrayList<>();
    // [추가] 회원 탈퇴 시 작성한 리뷰도 자동 삭제되도록 Cascade 적용

    @Builder
    private User(String email, String password, AuthProvider provider, String providerId, String nickname) {
        // [수정] 매개변수에 nickname 추가
        this.email = email;
        this.password = password;
        this.nickname = nickname;  // [수정] nickname 대입 추가
        this.provider = provider;
        this.providerId = providerId;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // 로컬 회원가입 생성 팩토리 메서드
    public static User createLocal(String email, String encodedPassword, String nickname) {
        // [수정] 매개변수에 nickname 추가
        return User.builder()
            .email(email)
            .password(encodedPassword)
            .nickname(nickname)  // [수정] 빌더에 nickname 추가
            .provider(AuthProvider.LOCAL)
            .build();
    }

    // OAuth2 회원가입 생성 팩토리 메서드
    public static User createOAuth(String email, AuthProvider provider, String providerId) {
        return User.builder()
            .email(email)
            .provider(provider)
            .providerId(providerId)
            .build();
    }
}
