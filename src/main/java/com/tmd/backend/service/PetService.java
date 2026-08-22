// 로그인한 사람의 이메일로 그 사람이 등록한 반려견 목록을 DB에서 찾아와, 프론트에 보여줄 수 있는 안전한 형태(PetResponse 리스트)로 가공해 주는 파일

package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.response.pet.PetResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j  // 로그(log.info 등) 찍을 수 있게 해줌

@RequiredArgsConstructor  // final 필드(petRepository, userRepository)를 자동으로 생성자 주입해줌

@Transactional(readOnly = true)  // 이 클래스의 메서드들은 기본적으로 DB를 읽기만 함 (조회 전용)

@Service  // 이 클래스가 Service 계층임을 Spring에게 알림

public class PetService {

    private final PetRepository petRepository;
    // 반려견 데이터를 DB에서 꺼내오는 도구

    private final UserRepository userRepository;
    // 사용자 데이터를 DB에서 꺼내오는 도구 (email로 user id를 알아내기 위해 필요)

    public List<PetResponse> getMyPets(String email) {
        // JWT에서 꺼낸 email을 받아서, 그 사람의 반려견 목록을 리턴하는 메서드

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        // email로 User를 찾음. 없으면 USER_NOT_FOUND 에러 던짐 (Pet은 userId로 찾아야 하는데, 우리가 가진 건 email뿐이라 User부터 찾아서 그 안의 id를 꺼내 쓰기 위함)

        List<Pet> pets = petRepository.findAllByUserId(user.getId());
        // 방금 찾은 User의 id로, 그 사람이 등록한 반려견들을 전부 조회
        // 결과는 Pet 여러 마리 (List)

        return pets.stream()
            // Pet 리스트를 하나씩 처리할 수 있는 흐름으로 바꿈

            .map(pet -> PetResponse.builder()
                .petId(pet.getId())
                .name(pet.getName())
                .breed(pet.getBreed())
                .size(pet.getSize())
                .imageUrl(pet.getImageUrl())
                .build())
            // Pet(원본, DB 그대로의 형태) 하나하나를 PetResponse(응답용, 프론트에 보여줄 형태)로 변환

            .toList();  // 변환된 PetResponse들을 다시 리스트로 모아서 리턴
    }
}
