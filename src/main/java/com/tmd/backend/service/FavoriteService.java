package com.tmd.backend.service;

import com.tmd.backend.common.ErrorCode;
import com.tmd.backend.domain.favorite.Favorite;
import com.tmd.backend.domain.pet.Pet;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.user.User;
import com.tmd.backend.dto.response.PageResponse;
import com.tmd.backend.dto.response.favorite.FavoriteResponse;
import com.tmd.backend.exception.BaseException;
import com.tmd.backend.repository.FavoriteRepository;
import com.tmd.backend.repository.PetRepository;
import com.tmd.backend.repository.PlaceRepository;
import com.tmd.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FavoriteService {
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final PlaceRepository placeRepository;
    private final PetRepository petRepository;
    private final MarkerColorService markerColorService;

    public PageResponse<FavoriteResponse> getMyFavorites(
        String email,
        Long petId,
        int page,
        int size
    ) {
        validatePageRequest(page, size);
        User user = findUser(email);
        Pet pet = petRepository.findByIdAndUserEmail(petId, email)
            .orElseThrow(() -> new BaseException(ErrorCode.NOT_OWNER_OF_DOG));
        PageRequest pageable = PageRequest.of(
            page,
            size,
            Sort.by(Sort.Order.desc("createdAt"), Sort.Order.desc("id"))
        );
        Page<Favorite> favorites = favoriteRepository.findAllByUserId(user.getId(), pageable);

        List<FavoriteResponse> content = favorites.stream()
            .map(favorite -> toResponse(favorite, pet))
            .toList();

        return new PageResponse<>(
            content,
            favorites.getNumber(),
            favorites.getSize(),
            favorites.getTotalElements(),
            favorites.getTotalPages(),
            favorites.hasNext()
        );
    }

    public boolean isFavorite(String email, Long placeId) {
        User user = findUser(email);
        return favoriteRepository.existsByUserIdAndPlaceId(user.getId(), placeId);
    }

    public Set<Long> getFavoritePlaceIds(String email, List<Long> placeIds) {
        if (placeIds.isEmpty()) return Set.of();
        return favoriteRepository.findPlaceIdsByUserEmailAndPlaceIdIn(email, placeIds)
            .stream()
            .collect(Collectors.toUnmodifiableSet());
    }

    @Transactional
    public void addFavorite(String email, Long placeId) {
        User user = findUser(email);
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new BaseException(ErrorCode.PLACE_NOT_FOUND));

        if (favoriteRepository.existsByUserIdAndPlaceId(user.getId(), placeId)) {
            throw new BaseException(ErrorCode.ALREADY_FAVORITE);
        }

        favoriteRepository.save(Favorite.create(user, place));
    }

    @Transactional
    public void deleteFavorite(String email, Long placeId) {
        User user = findUser(email);
        Favorite favorite = favoriteRepository.findByUserIdAndPlaceId(user.getId(), placeId)
            .orElseThrow(() -> new BaseException(ErrorCode.FAVORITE_NOT_FOUND));
        favoriteRepository.delete(favorite);
    }

    private FavoriteResponse toResponse(Favorite favorite, Pet pet) {
        Place place = favorite.getPlace();
        return FavoriteResponse.builder()
            .placeId(place.getId())
            .thumbnailUrl(place.getFirstImage())
            .title(place.getTitle())
            .addr(place.getAddr1())
            .markerColor(markerColorService.calculateMarkerColor(place.getPlacePetPolicy(), pet).name())
            .build();
    }

    private User findUser(String email) {
        return userRepository.findByEmail(email)
            .orElseThrow(() -> new BaseException(ErrorCode.USER_NOT_FOUND));
    }

    private void validatePageRequest(int page, int size) {
        if (page < 0 || size < 1 || size > 100) {
            throw new BaseException(ErrorCode.VALIDATION_ERROR);
        }
    }
}
