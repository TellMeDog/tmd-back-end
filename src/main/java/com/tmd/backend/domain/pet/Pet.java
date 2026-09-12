package com.tmd.backend.domain.pet;

import com.tmd.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JavaType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @JavaType(PetBreedJavaType.class)
    @JdbcTypeCode(SqlTypes.VARCHAR)
    @Column(nullable = false, columnDefinition = "varchar(50)")
    private PetBreed breed;

    @Column(nullable = false)
    private Double weight;

    @Column(name = "image_key")
    private String imageKey;

    @Column(nullable = false)
    private boolean hasMuzzle;

    @Column(nullable = false)
    private boolean hasLeash;

    @Column(nullable = false)
    private boolean hasCarrier;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Builder
    private Pet(User user, String name, PetBreed breed, Double weight, String imageKey,
                boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
        this.name = name;
        this.breed = breed;
        this.weight = weight;
        this.imageKey = imageKey;
        this.hasMuzzle = hasMuzzle;
        this.hasLeash = hasLeash;
        this.hasCarrier = hasCarrier;
        this.user = user;
    }
    public static Pet create(User user, String name, PetBreed breed, Double weight, String imageKey,
                             boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
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

    public void update(String name, PetBreed breed, Double weight, String imageKey,
                       Boolean hasMuzzle, Boolean hasLeash, Boolean hasCarrier) {
        if (name != null) this.name = name;
        if (breed != null) this.breed = breed;
        if (weight != null) this.weight = weight;
        this.imageKey = imageKey;
        if (hasMuzzle != null) this.hasMuzzle = hasMuzzle;
        if (hasLeash != null) this.hasLeash = hasLeash;
        if (hasCarrier != null) this.hasCarrier = hasCarrier;
    }
}
