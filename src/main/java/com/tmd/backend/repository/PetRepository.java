// DB의 pet 테이블에서, 특정 유저(userId)가 등록한 반려견들을 전부 찾아오는 검색 도구 파일
// 이 파일은 검색 기능만 준비해둔 것이고, 다음 단계인 PetService에서 이 도구를 실제로 사용하게 됨


package com.tmd.backend.repository;

import com.tmd.backend.domain.pet.Pet;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PetRepository extends JpaRepository<Pet, Long> {

    // Spring이 자동으로 이 userId를 가진 Pet을 전부 찾아라는 쿼리를 만들어줌
    // Pet은 한 사람당 여러 마리라 Optional이 아니라 List가 맞음
    // 필드명 user + 그 User의 id를 합쳐서 findByUserId로 인식
    List<Pet> findAllByUserId(Long userId);
}
