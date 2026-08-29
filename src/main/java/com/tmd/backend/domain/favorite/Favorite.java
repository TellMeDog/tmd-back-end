package com.tmd.backend.domain.favorite;

import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.user.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "favorite")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Favorite {

    @Id  // 이 필드가 테이블의 기본키(Primary Key)라고 표시
    // 이 필드가 고유 식별 번호(PK)이고, 그 번호는 우리가 직접 안 정하고 DB가 자동으로 1씩 늘려가며 매겨준다는 뜻
    @GeneratedValue(strategy = GenerationType.IDENTITY)

    private Long id;  // 즐겨찾기 고유 번호 (PK)

    @ManyToOne(fetch = FetchType.LAZY)  // 여러 개의 Favorite이 한 명의 User에 속한다는 관계를 표현
    @JoinColumn(name = "user_id", nullable = false)  // 실제 DB에서는 이 관계가 "user_id"라는 컬럼으로 저장됨, nullable = false → 사용자 없는 즐겨찾기는 저장 불가
    private User user;  // 이 즐겨찾기를 누른 사용자 (FK: user_id)

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "place_id", nullable = false)  // nullable = false → 장소 없는 즐겨찾기는 저장 불가
    private Place place;  // 즐겨찾기 된 장소 (FK: place_id)

    @Column(nullable = false, updatable = false)  // nullable = false → 항상 값이 있어야 함 (onCreate()가 자동으로 채워줌), updatable = false → 한번 저장된 후엔 이 값이 수정되지 않음
    private LocalDateTime createdAt;  // 즐겨찾기 등록 시각


    // [빌더 패턴]
    // 예: Favorite.builder().user(user).place(place).build()
    // 값을 하나씩 이름 붙여서 채우고 마지막에 build()로 객체 완성하는 방식
    @Builder
    private Favorite(User user, Place place) {
        this.user = user;
        this.place = place;
    }
    // Favorite 객체를 만드는 생성자
    // user, place만 받으면 됨 (id는 자동 생성, createdAt은 아래에서 자동 채움)

    @PrePersist  // 이 Favorite 객체가 DB에 처음 저장되기 직전에, 자동으로 이 메서드를 실행하라는 신호
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
    // DB에 저장되기 직전에 자동으로 현재 시각을 createdAt에 채움
}
