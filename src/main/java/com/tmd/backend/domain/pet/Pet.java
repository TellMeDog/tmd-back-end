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

    private String breed;  // [수정] PetBreed(Enum) → String으로 변경

    private Double size;  // [수정] PetSize(Enum) → Double로 변경

    private String imageUrl;

    private boolean hasMuzzle;      // 입마개 유무 추가
    private boolean hasLeash;        // 목줄 유무 추가
    private boolean hasCarrier;       // 이동장 유무 추가
    // [수정] isVaccinated 필드 제거 (예방접종 유무)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name="user_id")
    private User user;

    @Builder
    public Pet(User user, String name, String breed, Double size, String imageUrl,
                      boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
        // [수정] 매개변수: PetBreed→String, PetSize→Double, isVaccinated 매개변수 제거

        this.user = user;
        this.name = name;
        this.breed = breed;
        this.size = size;
        this.imageUrl = imageUrl;
        this.hasMuzzle = hasMuzzle;
        this.hasLeash = hasLeash;
        this.hasCarrier = hasCarrier;
    }

    public static Pet create(User user, String name, String breed, Double size, String imageUrl,
                             boolean hasMuzzle, boolean hasLeash, boolean hasCarrier) {
        // [수정] 매개변수: PetBreed→String, PetSize→Double, isVaccinated 매개변수 제거
        return Pet.builder()
            .user(user)
            .name(name)
            .breed(breed)
            .size(size)
            .imageUrl(imageUrl)
            .hasMuzzle(hasMuzzle)
            .hasLeash(hasLeash)
            .hasCarrier(hasCarrier)
            .build();
    }

    // 반려견 수정(update) 메서드 추가
    public void update(String name, String breed, Double size, String imageUrl,
                       Boolean hasMuzzle, Boolean hasLeash, Boolean hasCarrier) {
        // [수정] 매개변수: PetBreed→String, PetSize→Double, isVaccinated 매개변수 제거
        // "이 반려견의 정보를 바꿔달라"는 요청을 받는 메서드
        // 매개변수로 새 값들을 받음 (일부는 null일 수 있음)

        if (name != null) this.name = name;
        // name이 null이 아니면(=바꾸고 싶은 값을 보냈으면) 이 Pet 객체의 name을 새 값으로 바꿈
        // null이면 (안 보냈으면) 아무것도 안 하고 기존 값 그대로 둠
        // 아래도 똑같은 방식
        if (breed != null) this.breed = breed;
        if (size != null) this.size = size;
        if (imageUrl != null) this.imageUrl = imageUrl;
        if (hasMuzzle != null) this.hasMuzzle = hasMuzzle;
        if (hasLeash != null) this.hasLeash = hasLeash;
        if (hasCarrier != null) this.hasCarrier = hasCarrier;
    }
}
