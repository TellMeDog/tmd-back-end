// 로그인한 사용자가 즐겨찾기한 장소들을, DB에서 페이지 단위로 조회해서 프론트에 보여줄 응답 형태로 가공하는 파일

package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.favorite.Favorite;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.favorite.FavoriteResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.FavoriteRepository;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j  // 로그(log.info 등) 찍을 수 있게 해줌
@RequiredArgsConstructor  // final 필드들을 자동으로 생성자 주입해줌
@Transactional(readOnly = true)  // 이 클래스의 메서드들은 기본적으로 DB를 "읽기만" 함
@Service  // 이 클래스가 Service 계층임을 Spring에게 알림

public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    // 즐겨찾기 데이터를 DB에서 꺼내오는 도구

    private final UserRepository userRepository;
    // 사용자 데이터를 DB에서 꺼내오는 도구


    public PageResponse<FavoriteResponse> getMyFavorites(String email, int page, int size) {
        // "내 즐겨찾기 목록"을 조회하는 메서드

        User user = userRepository.findByEmail(email)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
        // email로 User를 찾음. 없으면 USER_NOT_FOUND 에러

        Pageable pageable = PageRequest.of(page, size);
        // page, size 숫자를 Spring이 이해하는 "페이징 요청 객체"로 변환

        Page<Favorite> favorites = favoriteRepository.findAllByUserId(user.getId(), pageable);
        // 이 사용자의 즐겨찾기 목록을, 요청한 페이지만큼 DB에서 조회

        List<FavoriteResponse> content = favorites.stream()
            // 조회된 Favorite들을 하나씩 처리할 준비

            .map(favorite -> FavoriteResponse.builder()
                .placeId(favorite.getPlace().getId())
                .contentId(favorite.getPlace().getContentId())
                .thumbnailUrl(favorite.getPlace().getFirstImage())
                .title(favorite.getPlace().getTitle())
                .addr(favorite.getPlace().getAddr1())
                .markerColor(null)
                // TODO: 지도 담당자가 Place에 markerColor 계산 로직을 만들면 연결 필요
                .build())
            // 하나의 Favorite(원본)을 FavoriteResponse(응답용)로 변환

            .toList();
        // 변환된 것들을 리스트로 모음

        return new PageResponse<>(content, (int) favorites.getTotalElements(), favorites.getTotalPages());
        // 변환된 목록과 페이징 정보를 합쳐서 최종 응답 완성
    }
}
