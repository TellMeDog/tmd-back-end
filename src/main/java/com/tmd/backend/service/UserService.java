// 로그인한 사용자의 이메일을 받아 DB에서 그 사람을 찾아 안전한 형태(UserResponse)로 가공해 주는, 마이페이지의 두뇌 역할을 하는 파일

package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.favorite.Favorite;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.response.user.UserResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.FavoriteRepository;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
// 이 클래스 안에서 log.info(...), log.warn(...) 같은 로그 코드를 쓸 수 있게 해 줌

@RequiredArgsConstructor
// 아래 private final 필드(userRepository)를 자동으로 주입받는
// 생성자를 자동 생성해 줌 (직접 생성자를 안 써도 됨)

@Transactional(readOnly = true)
// 이 클래스의 모든 메서드는 기본적으로 DB를 읽기만 한다는 설정
// (조회만 하는 메서드 성능에 유리함, AuthService와 동일한 패턴)

@Service
// 이 클래스가 Service 계층임을 Spring에게 알림
// 이래야 Controller에서 이 클래스를 가져다 쓸 수 있게 Spring이 관리해 줌

public class UserService {

    private final UserRepository userRepository;
    // Repository를 필드로 선언. final이라 한번 정해지면 안 바뀜
    // @RequiredArgsConstructor 덕분에 이 필드는 자동으로 생성자 주입됨

    // 회원탈퇴 시 pet, favorite 엔티티 연쇄 삭제 위해 작성
    private final PetRepository petRepository;
    private final FavoriteRepository favoriteRepository;

    public UserResponse getMyInfo(String email) {
        // 이메일을 받아서 UserResponse를 리턴하는 메서드 시작
        // 이 email은 나중에 Controller가 JWT에서 꺼내서 넘겨줄 예정

        User user = userRepository.findByEmail(email)
            // Repository의 findByEmail 메서드 호출
            // 결과는 Optional<User> (있을 수도, 없을 수도 있는 상태)

            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        // 만약 Optional이 비어있으면(못 찾았으면) USER_NOT_FOUND 에러를 던지고 메서드 실행을 중단함
        // 찾았으면 User 객체가 그대로 user 변수에 저장됨

        return UserResponse.builder()
            // UserResponse를 빌더 패턴으로 만들기 시작

            .id(user.getId())
            // user 객체에서 id 값을 꺼내서 UserResponse의 id 칸에 넣음

            .email(user.getEmail())
            // user 객체에서 email 값을 꺼내서 UserResponse의 email 칸에 넣음

            .nickname(user.getNickname())  // [수정] 빌더에 nickname 추가

            .provider(user.getProvider().name())
            // user.getProvider()는 Enum 타입(AuthProvider)을 리턴함
            // .name()을 붙여서 그 Enum 값을 문자열로 변환 (예: "LOCAL")
            // UserResponse의 provider 필드가 String 타입이라 이렇게 변환 필요

            .createdAt(user.getCreatedAt())
            // user 객체에서 가입일시 값을 꺼내서 그대로 넣음

            .build();
        // 지금까지 채운 값들로 실제 UserResponse 객체를 완성해서 리턴
    }


    // 회원 탈퇴 메서드 추가 - 이후에 cascade로 구현한 pet은 주석 처리함
    // (pet, review는 cascade고 favorite은 여기서 삭제)
    @Transactional
    public void withdraw(String email) {
        User user = userRepository.findByEmail(email)  // email로 탈퇴할 User를 찾음
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

//        List<Pet> pets = petRepository.findByUserId(user.getId());
//        petRepository.deleteAll(pets);  // 이 사용자가 등록한 반려견들을 먼저 전부 삭제

        // Pageable.unpaged() = 페이징 없이 전체 다 가져오라는 뜻
        Page<Favorite> favorites = favoriteRepository.findAllByUserId(user.getId(), Pageable.unpaged());
        favoriteRepository.deleteAll(favorites);  // 이 사용자의 즐겨찾기들도 먼저 전부 삭제

        userRepository.delete(user);  // 연관된 데이터를 다 지운 뒤, 마지막으로 User 자체를 삭제
    }
}
