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
}
