// Repository에서 findByEmail로 "찾아온 데이터를 어떻게 포장해서 보낼지" 정하는 클래스

package com.tmd.backend.dto.response.user;

import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

// 클래스 안 모든 private 필드에 대해 자동으로 getter 메서드를 만들어줌 (읽기만 가능하고 수정은 안 됨)
@Getter

// No Args = "매개변수(인자) 없는" 생성자를 자동으로 만들어줌
// 즉 UserResponse() { } 라는 빈 생성자가 자동 생성됨
@NoArgsConstructor(access = AccessLevel.PROTECTED) // "일단 빈 객체를 만들고 나중에 값 채우기" 방식을 쓸 때가 있으니까 일단 PROTECTED
public class UserResponse {      // Controller가 이 클래스를 써야 하니 public
    // 필드 선언 (창고 목록 만들기)
    // private이니까 @Getter로 생긴 getId(), getEmail() 등을 통해서만 읽기 가능
    private Long id;
    private String email;
    private String nickname;  // [수정] 필드 추가: 마이페이지 조회 시 닉네임도 보여주기 위함
    private String provider;
    private LocalDateTime createdAt;

    @Builder
    public UserResponse(Long id, String email, String nickname, String provider, LocalDateTime createdAt) {      // 이 4개 값을 받아서 UserResponse 객체 하나 만드는 법 정의
        // 생성자 안의 대입문 (실제로 서랍에 물건 넣기)
        // [수정] 매개변수에 nickname 추가

        this.id = id; // 매개변수로 받은 값을 클래스의 필드에 실제로 저장하는 코드
        this.email = email;
        this.nickname = nickname;  // [수정] nickname 대입 추가
        this.provider = provider;
        this.createdAt = createdAt;
    }
}
