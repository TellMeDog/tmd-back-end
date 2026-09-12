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
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(value = EnumType.STRING)
    private PetBreed breed;

    private Integer weight;

    @Column(name = "image_key")
    private String imageKey;

    private boolean hasMuzzle;      // 입마개 유무 추가
    private boolean hasLeash;        // 목줄 유무 추가
    private boolean hasCarrier;       // 이동장 유무 추가

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Builder
    public Pet(User user, String name, PetBreed breed, Integer weight, String imageKey, boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
        this.name = name;
        this.breed = breed;
        this.weight = weight;
        this.imageKey = imageKey;
        this.hasMuzzle = hasMuzzle;
        this.hasLeash = hasLeash;
        this.hasCarrier = hasCarrier;
        this.user = user;
    }


    public static Pet create(User user, String name, PetBreed breed, Integer weight, String imageKey, boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
        return Pet.builder()
            .user(user)
            .name(name)
            .breed(breed)
            .weight(weight)
            .imageKey(imageKey)
            .hasMuzzle(hasMuzzle)
            .hasLeash(hasLeash)
            .hasCarrier(hasCarrier)
            .build();
    }

    public void update(String name, PetBreed breed, Integer weight, String imageKey,
                       Boolean hasMuzzle, Boolean hasLeash, Boolean hasCarrier) {
        if (name != null) this.name = name;
        if (breed != null) this.breed = breed;
        if (weight != null) this.weight = weight;
        if (imageKey != null) this.imageKey = imageKey;
        if (hasMuzzle != null) this.hasMuzzle = hasMuzzle;
        if (hasLeash != null) this.hasLeash = hasLeash;
        if (hasCarrier != null) this.hasCarrier = hasCarrier;
    }
}
