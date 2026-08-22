package com.tmd.backend.domain.pet;

import com.tmd.backend.domain.user.User; // User Entity를 가져옴 (Pet이 User를 참조해야 하므로 필요)
import jakarta.persistence.*;
import lombok.*;


@Entity // 이 클래스는 DB 테이블과 매핑되는 Entity다 라는 뜻
@Table(name = "pet") // 매핑될 실제 테이블 이름을 지정. pet 테이블과 연결됨
@Getter // 모든 필드에 대해 자동으로 getter 메서드 생성
@NoArgsConstructor(access = AccessLevel.PROTECTED)
// 매개변수 없는 빈 생성자를 자동 생성하되, 외부에서 함부로 못 쓰게 PROTECTED로 제한

public class Pet {

    @Id
    // 이 필드가 테이블의 기본키(Primary Key)임을 표시

    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // id 값을 DB가 자동으로 1씩 증가시켜서 채워줌 (auto increment)

    private Long id;
    // Pet의 고유 번호

    @ManyToOne(fetch = FetchType.LAZY)
    // 여러 개의 Pet이 한 명의 User에 속한다는 관계를 표현
    // LAZY: User 정보는 실제로 필요할 때만 조회 (성능 최적화)

    @JoinColumn(name = "user_id", nullable = false)
    // 실제 DB에서는 이 관계가 "user_id"라는 컬럼으로 저장됨
    // nullable = false: 반드시 주인(User)이 있어야 함 (주인 없는 반려견 불가)

    private User user;
    // 이 반려견의 주인 (User 객체 자체를 참조)

    @Column(nullable = false, length = 50)
    private String name;

    @Column(nullable = false, length = 50)
    private String breed;

    @Column(nullable = false, length = 20)
    private String size;

    @Column(name = "image_url", length = 500)
    private String imageUrl;
    // 프로필 사진 URL, DB 컬럼명은 image_url(스네이크케이스)로 매핑
    // 자바 필드명은 imageUrl(카멜케이스) - JPA가 자동으로 변환해줌

    @Builder
    // 아래 생성자를 빌더 패턴으로도 쓸 수 있게 해줌
    // 예: Pet.builder().user(user).name("초코")....build()

    private Pet(User user, String name, String breed, String size, String imageUrl) {
        this.user = user;
        this.name = name;
        this.breed = breed;
        this.size = size;
        this.imageUrl = imageUrl;
        // 매개변수로 받은 값들을 실제 필드에 저장
    }
}
