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

    @Column(name = "image_url")
    private String imageKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;

    @Builder
    public Pet(User user, String name, PetBreed breed, PetSize size, String imageKey) {
        this.user = user;
        this.name = name;
        this.breed = breed;
        this.size = size;
        this.imageKey = imageKey;
    }

    public static Pet create(User user, String name, PetBreed breed, PetSize size, String imageKey) {
        return Pet.builder()
            .user(user)
            .name(name)
            .breed(breed)
            .size(size)
            .imageKey(imageKey)
            .build();
    }

    public void update(String name, PetBreed breed, PetSize size, String imageKey) {
        if (name != null) this.name = name;
        if (breed != null) this.breed = breed;
        if (size != null) this.size = size;
        this.imageKey = imageKey;
    }
}
