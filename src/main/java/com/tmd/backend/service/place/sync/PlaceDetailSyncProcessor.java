package com.tmd.backend.service.place.sync;

import com.tmd.backend.service.place.PlacePetPolicyClassifier;

import com.tmd.backend.ai.PetPolicyAnalysis;
import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.domain.place.PlacePetPolicy;
import com.tmd.backend.domain.place.PlacePetPolicyReview;
import com.tmd.backend.external.tourapi.TourApiClient;
import com.tmd.backend.external.tourapi.TourApiPetInfoItem;
import com.tmd.backend.repository.place.PlacePetInfoRepository;
import com.tmd.backend.repository.place.PlacePetPolicyRepository;
import com.tmd.backend.repository.place.PlacePetPolicyReviewRepository;
import com.tmd.backend.repository.place.PlaceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class PlaceDetailSyncProcessor {
    private final TourApiClient tourApiClient;
    private final PlaceRepository placeRepository;
    private final PlacePetInfoRepository infoRepository;
    private final PlacePetPolicyRepository policyRepository;
    private final PlacePetPolicyReviewRepository reviewRepository;
    private final PlacePetPolicyClassifier classifier;
    private final ObjectMapper objectMapper;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public DetailResult process(Long placeId) {
        Place place = placeRepository.findById(placeId)
            .orElseThrow(() -> new IllegalStateException("동기화 대상 장소가 없습니다: " + placeId));
        if (!place.isActive()) return DetailResult.SKIPPED;

        TourApiPetInfoItem item = tourApiClient.getPetTourInfo(place.getContentId());
        PetInfoStatus status = item == null ? PetInfoStatus.NO_DATA : PetInfoStatus.SUCCESS;
        PlacePetInfo info = infoRepository.findByPlaceId(placeId).orElse(null);
        if (info == null) {
            info = item == null ? PlacePetInfo.empty(place, status) : PlacePetInfo.from(place, item, status);
            infoRepository.save(info);
        } else {
            info.update(item, status);
        }

        PlacePetPolicyClassifier.Classification classification = classifier.classify(info);
        PetPolicyAnalysis suggestion = classification.analysis();
        PlacePetPolicy policy = policyRepository.findByPlaceId(placeId).orElse(null);
        boolean reviewCreated = false;
        if (classification.requiresReview()) {
            if (policy == null) {
                policy = PlacePetPolicy.from(place, suggestion);
                policyRepository.save(policy);
            }
            policy.markReviewPending();
            String sourceVersion = Objects.requireNonNullElse(place.getModifiedTime(), "UNKNOWN");
            if (!reviewRepository.existsByPlaceIdAndSourceModifiedTime(placeId, sourceVersion)) {
                reviewRepository.save(PlacePetPolicyReview.pending(
                    place,
                    sourceVersion,
                    String.join(",", classification.reviewReasons()),
                    objectMapper.writeValueAsString(suggestion)
                ));
                reviewCreated = true;
            }
        } else if (policy == null) {
            policyRepository.save(PlacePetPolicy.from(place, suggestion));
        } else {
            policy.update(suggestion);
        }
        place.markPetInfoSynced();
        return new DetailResult(status, true, reviewCreated);
    }

    public record DetailResult(PetInfoStatus status, boolean policyUpdated, boolean reviewCreated) {
        private static final DetailResult SKIPPED = new DetailResult(null, false, false);
    }
}
