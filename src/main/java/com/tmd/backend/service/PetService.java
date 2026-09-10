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

import com.tmd.backend.dto.request.pet.PetRegisterRequest;  // 반려견 추가 코드를 위해 추가
import com.tmd.backend.dto.request.pet.PetUpdateRequest;   // 반려견 수정 코드를 위해 추가

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

        List<Pet> pets = petRepository.findByUserId(user.getId());
        // 수정: findAllByUserId → findByUserId (병합된 Repository 메서드명에 맞춤)
        // 방금 찾은 User의 id로, 그 사람이 등록한 반려견들을 전부 조회
        // 결과는 Pet 여러 마리 (List)

        return pets.stream()  // Pet 리스트를 하나씩 처리할 수 있는 흐름으로 바꿈
            .map(pet -> PetResponse.builder()
                .petId(pet.getId())
                .name(pet.getName())
                .breed(pet.getBreed())
                // [수정] .breed(pet.getBreed().name()) → .breed(pet.getBreed())
                //        breed가 이제 String이라 .name() 호출(Enum→문자열 변환) 불필요
                .size(pet.getSize())
                // [수정] .size(pet.getSize().name()) → .size(pet.getSize())
                //        size가 이제 Double이라 .name() 호출 불필요
                .imageUrl(pet.getImageUrl())
                .hasMuzzle(pet.isHasMuzzle())
                .hasLeash(pet.isHasLeash())
                .hasCarrier(pet.isHasCarrier())
                // [수정] .isVaccinated(pet.isVaccinated()) 줄 제거
                .build())
                // Pet(원본, DB 그대로의 형태) 하나하나를 PetResponse(응답용, 프론트에 보여줄 형태)로 변환

            .toList();  // 변환된 PetResponse들을 다시 리스트로 모아서 리턴
    }

    // 반려견 - 반려견 추가를 위해 아래 메서드 추가
    // 아래 메서드는 쓰기(등록) 작업이라, 클래스 기본값(readOnly=true)을 무시하고 이 메서드에만 별도로 쓰기 가능한 트랜잭션을 적용함
    @Transactional
    // email(로그인한 사람)과 requests(등록하려는 반려견 정보 리스트)를 받아서 등록 완료된 반려견들의 정보(PetResponse 리스트)를 리턴하는 메서드
    public List<PetResponse> registerPets(String email, List<PetRegisterRequest> requests) {
        User user = userRepository.findByEmail(email) // email로 User를 찾음. 이 반려견들의 주인을 알아내기 위함
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        List<Pet> pets = requests.stream() // 등록 요청 리스트(requests)를 하나씩 처리할 준비
            .map(req -> Pet.builder()  // Pet.java에서 이미 Breed 타입을 받도록 고쳤음
                .user(user) // 방금 찾은 user를 이 반려견의 주인으로 연결
                .name(req.getName())  // 요청에서 받은 이름을 그대로 사용
                .breed(req.getBreed()) // 요청에서 받은 품종을 그대로 사용  // req.getBreed()는 이제 PetRegisterRequest에서 이미 Breed(Enum)로 받고 있음
                .size(req.getSize()) // 요청에서 받은 크기를 그대로 사용
                .imageUrl(req.getImageUrl()) // 요청에서 받은 이미지 URL (없으면 null, 필수 아님)
                .hasMuzzle(req.isHasMuzzle())
                .hasLeash(req.isHasLeash())
                .hasCarrier(req.isHasCarrier())
                // [수정] .isVaccinated(req.isVaccinated()) 줄 제거
                .build()) // 위 값들로 Pet 객체 하나를 완성 (아직 DB엔 저장 안 된 상태, 메모리 상에서만 만들어진 상태)
            .toList(); // req(요청) 하나하나를 Pet 객체로 바꾼 결과들을 리스트로 모음

        List<Pet> savedPets = petRepository.saveAll(pets);
        // 방금 만든 Pet 리스트를 DB에 한번에 저장
        // saveAll은 JpaRepository가 기본으로 제공하는 메서드 (직접 안 만들어도 있음)
        // 저장이 끝나면, 각 Pet에 DB가 자동으로 매긴 id 값이 채워져서 돌아옴
        // (저장 전엔 id가 비어있었는데, 저장 후엔 실제 번호가 생김)

        return savedPets.stream() // 방금 저장된(id가 채워진) Pet들을 다시 하나씩 처리
            .map(pet -> PetResponse.builder()
                .petId(pet.getId()) // DB가 방금 매겨준 진짜 id 값
                .name(pet.getName())
                .breed(pet.getBreed())  // [수정] .name() 호출 제거
                .size(pet.getSize())  // [수정] .name() 호출 제거
                .imageUrl(pet.getImageUrl())
                .hasMuzzle(pet.isHasMuzzle())
                .hasLeash(pet.isHasLeash())
                .hasCarrier(pet.isHasCarrier())
                // [수정] isVaccinated 줄 제거
                .build()) // 저장된 Pet(원본 Entity)을 PetResponse(응답용 DTO)로 변환

            .toList(); // 변환된 것들을 다시 리스트로 모아서 최종 리턴
    }


    // 반려견 - 반려견 수정을 위해 아래 메서드 추가
    // 아래 메서드는 쓰기(수정) 작업이라 클래스 기본값(readOnly=true) 대신 별도 적용
    @Transactional

    public PetResponse updatePet(String email, Long petId, PetUpdateRequest request) {
        // 로그인한 사람 email, 수정할 반려견 id(petId), 새 값들(request)을 받음

        User user = userRepository.findByEmail(email)  // 요청 보낸 사람이 누군지 찾음 (소유권 확인에 필요)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Pet pet = petRepository.findById(petId)
            // petId로 수정할 반려견을 찾음, 없으면 PET_NOT_FOUND
            .orElseThrow(() -> new BaseException(ErrorCode.PET_NOT_FOUND));

        if (!pet.getUser().getId().equals(user.getId())) {   // 이 반려견의 주인과 지금 요청 보낸 사람이 다르면 FORBIDDEN 에러
            throw new BaseException(ErrorCode.FORBIDDEN);
        }

        pet.update(  // 검증 통과했으면 Pet.java에 만든 update() 메서드로 실제 값 변경 (null인 필드는 기존 값 유지됨)
            request.getName(),
            request.getBreed(),
            request.getSize(),
            request.getImageUrl(),
            request.getHasMuzzle(),
            request.getHasLeash(),
            request.getHasCarrier()
            // [수정] request.getIsVaccinated() 인자 제거
            //        (Pet.update() 메서드 자체도 매개변수에서 isVaccinated 제거했으므로 맞춰야 함)
        );

        return PetResponse.builder()
            .petId(pet.getId())
            .name(pet.getName())
            .breed(pet.getBreed())  // [수정] .name() 호출 제거
            .size(pet.getSize())  // [수정] .name() 호출 제거
            .imageUrl(pet.getImageUrl())
            .hasMuzzle(pet.isHasMuzzle())
            .hasLeash(pet.isHasLeash())
            .hasCarrier(pet.isHasCarrier())
            // [수정] isVaccinated 줄 제거
            .build();
    }
        // 수정된 최신 정보를 응답 형태로 만들어서 리턴


    // 반려견 - 반려견 삭제를 위해 아래 메서드 추가
    @Transactional
    public void deletePet(String email, Long petId) {
        User user = userRepository.findByEmail(email)  // email로 User 조회 (요청자가 누구인지)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));

        Pet pet = petRepository.findById(petId)  // petId로 삭제할 Pet 조회 (없으면 PET_NOT_FOUND)
            .orElseThrow(() -> new BaseException(ErrorCode.PET_NOT_FOUND));

        if (!pet.getUser().getId().equals(user.getId())) {  // 이 반려견의 진짜 주인과 요청자가 같은지 확인
            throw new BaseException(ErrorCode.FORBIDDEN);
        }

        petRepository.delete(pet);  // 실제 삭제
    }
}
