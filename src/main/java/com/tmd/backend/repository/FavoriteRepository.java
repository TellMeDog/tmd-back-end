// DB의 favorite 테이블에서 특정 사용자와 장소를 기준으로 즐겨찾기 데이터를 찾고, 확인하고, 페이징된 목록으로 조회하는 검색 파일

package com.tmd.backend.repository;

import com.tmd.backend.domain.favorite.Favorite;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {

    Optional<Favorite> findByUserIdAndPlaceId(Long userId, Long placeId);
    // 특정 사용자가 특정 장소를 즐겨찾기했는지 찾는 기능

    boolean existsByUserIdAndPlaceId(Long userId, Long placeId);
    // 이미 즐겨찾기 되어있는지 true/false로 확인

    Page<Favorite> findAllByUserId(Long userId, Pageable pageable);
    // 특정 사용자의 즐겨찾기 목록을 페이징해서 조회
}
