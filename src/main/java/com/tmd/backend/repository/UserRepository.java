package com.tmd.backend.repository;

import com.tmd.backend.domain.user.*;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmailAndProvider(String email, AuthProvider provider);
    boolean existsByEmailAndProvider(String email, AuthProvider provider);
    Optional<User> findByProviderAndProviderId(AuthProvider provider, String providerId);

    // "DB에서 데이터를 꺼내오는 방법"을 추가하는 코드
    // email 컬럼으로 찾기 명령 -> spring이 인식해서 SQL 생성해 줌
    // Optional<User>: User가 있을 수도 없을 수도 있다 -> NullPointerException 방지
    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);
    // provider 상관없이, 이 email 자체가 이미 존재하는지 확인
    // email 중복 가입 방지 로직에 사용하려고 추가
}
