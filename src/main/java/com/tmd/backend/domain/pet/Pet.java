package com.tmd.backend.domain.pet;

import com.tmd.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(value = EnumType.STRING)
    private PetBreed breed;

    @Enumerated(value = EnumType.STRING)
    private PetSize size;

    private String imageUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;

    @Builder
    public Pet(User user, String name, PetBreed breed, PetSize size, String imageUrl) {
        this.user = user;
        this.name = name;
        this.breed = breed;
        this.size = size;
        this.imageUrl = imageUrl;
    }

    public static Pet create(User user, String name, PetBreed breed, PetSize size, String imageUrl) {
        return Pet.builder()
            .user(user)
            .name(name)
            .breed(breed)
            .size(size)
            .imageUrl(imageUrl)
            .build();
    }

    // 반려견 수정(update) 메서드 추가
    public void update(String name, PetBreed breed, PetSize size, String imageUrl) {
        // "이 반려견의 정보를 바꿔달라"는 요청을 받는 메서드
        // 매개변수로 새 값들을 받음 (일부는 null일 수 있음)

        if (name != null) this.name = name;
        // name이 null이 아니면(=바꾸고 싶은 값을 보냈으면) 이 Pet 객체의 name을 새 값으로 바꿈
        // null이면 (안 보냈으면) 아무것도 안 하고 기존 값 그대로 둠
        // 아래 3개도 똑같은 방식
        if (breed != null) this.breed = breed;
        if (size != null) this.size = size;
        if (imageUrl != null) this.imageUrl = imageUrl;
    }
}
