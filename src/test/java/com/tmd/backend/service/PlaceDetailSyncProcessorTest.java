package com.tmd.backend.service;

import com.tmd.backend.ai.AccessScope;
import com.tmd.backend.ai.PetPolicyAnalysis;
import com.tmd.backend.ai.WeightLimitType;
import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.domain.place.PlacePetPolicy;
import com.tmd.backend.domain.place.PlacePetPolicyReview;
import com.tmd.backend.external.TourApiClient;
import com.tmd.backend.external.TourApiPetInfoItem;
import com.tmd.backend.repository.PlacePetInfoRepository;
import com.tmd.backend.repository.PlacePetPolicyRepository;
import com.tmd.backend.repository.PlacePetPolicyReviewRepository;
import com.tmd.backend.repository.PlaceRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import tools.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PlaceDetailSyncProcessorTest {
    private final TourApiClient tourApiClient = mock(TourApiClient.class);
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final PlacePetInfoRepository infoRepository = mock(PlacePetInfoRepository.class);
    private final PlacePetPolicyRepository policyRepository = mock(PlacePetPolicyRepository.class);
    private final PlacePetPolicyReviewRepository reviewRepository = mock(PlacePetPolicyReviewRepository.class);
    private final PlacePetPolicyClassifier classifier = mock(PlacePetPolicyClassifier.class);
    private final ObjectMapper objectMapper = mock(ObjectMapper.class);
    private final PlaceDetailSyncProcessor processor = new PlaceDetailSyncProcessor(
        tourApiClient,
        placeRepository,
        infoRepository,
        policyRepository,
        reviewRepository,
        classifier,
        objectMapper
    );

    private Place place;
    private PlacePetInfo info;

    @BeforeEach
    void setUp() {
        place = Place.create("content-1", null, null, null, "장소", 127.0, 37.0,
            null, null, "20260916030000", null, null, null, null, null);
        ReflectionTestUtils.setField(place, "id", 1L);
        info = PlacePetInfo.empty(place, PetInfoStatus.NO_DATA);
        when(placeRepository.findById(1L)).thenReturn(Optional.of(place));
        when(infoRepository.findByPlaceId(1L)).thenReturn(Optional.of(info));
    }

    @Test
    void 명확한_정책은_기존_정책을_갱신하고_동기화_버전을_기록한다() {
        TourApiPetInfoItem item = new TourApiPetInfoItem();
        PlacePetPolicy policy = PlacePetPolicy.from(place, analysis(AccessScope.UNKNOWN));
        when(tourApiClient.getPetTourInfo("content-1")).thenReturn(item);
        when(policyRepository.findByPlaceId(1L)).thenReturn(Optional.of(policy));
        when(classifier.classify(info)).thenReturn(classification(AccessScope.ALL, List.of()));

        PlaceDetailSyncProcessor.DetailResult result = processor.process(1L);

        assertThat(result.status()).isEqualTo(PetInfoStatus.SUCCESS);
        assertThat(result.policyUpdated()).isTrue();
        assertThat(result.reviewCreated()).isFalse();
        assertThat(policy.getAccessScope()).isEqualTo(AccessScope.ALL);
        assertThat(policy.getReviewPending()).isFalse();
        assertThat(place.getPetInfoSyncedModifiedTime()).isEqualTo("20260916030000");
        verify(reviewRepository, never()).save(any());
    }

    @Test
    void 검토가_필요하면_기존_정책을_보존하고_버전별_검토를_생성한다() {
        TourApiPetInfoItem item = new TourApiPetInfoItem();
        PlacePetPolicy policy = PlacePetPolicy.from(place, analysis(AccessScope.ALL));
        when(tourApiClient.getPetTourInfo("content-1")).thenReturn(item);
        when(policyRepository.findByPlaceId(1L)).thenReturn(Optional.of(policy));
        when(classifier.classify(info)).thenReturn(classification(AccessScope.PARTIAL, List.of("scope_conflict")));
        when(reviewRepository.existsByPlaceIdAndSourceModifiedTime(1L, "20260916030000")).thenReturn(false);
        when(objectMapper.writeValueAsString(any(PetPolicyAnalysis.class))).thenReturn("{\"placeId\":1}");

        PlaceDetailSyncProcessor.DetailResult result = processor.process(1L);

        assertThat(result.reviewCreated()).isTrue();
        assertThat(policy.getAccessScope()).isEqualTo(AccessScope.ALL);
        assertThat(policy.getReviewPending()).isTrue();
        assertThat(place.getPetInfoSyncedModifiedTime()).isEqualTo("20260916030000");
        verify(reviewRepository).save(any(PlacePetPolicyReview.class));
    }

    @Test
    void 같은_원본_버전의_검토는_중복_생성하지_않는다() {
        PlacePetPolicy policy = PlacePetPolicy.from(place, analysis(AccessScope.ALL));
        when(policyRepository.findByPlaceId(1L)).thenReturn(Optional.of(policy));
        when(classifier.classify(info)).thenReturn(classification(AccessScope.PARTIAL, List.of("scope_conflict")));
        when(reviewRepository.existsByPlaceIdAndSourceModifiedTime(1L, "20260916030000")).thenReturn(true);

        PlaceDetailSyncProcessor.DetailResult result = processor.process(1L);

        assertThat(result.reviewCreated()).isFalse();
        assertThat(policy.getReviewPending()).isTrue();
        verify(reviewRepository, never()).save(any());
        verify(objectMapper, never()).writeValueAsString(any());
    }

    @Test
    void 외부_API가_실패하면_동기화_버전을_기록하지_않는다() {
        when(tourApiClient.getPetTourInfo("content-1")).thenThrow(new IllegalStateException("timeout"));

        assertThatThrownBy(() -> processor.process(1L))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("timeout");

        assertThat(place.getPetInfoSyncedModifiedTime()).isNull();
        verify(infoRepository, never()).save(any());
        verify(policyRepository, never()).save(any());
    }

    private PlacePetPolicyClassifier.Classification classification(AccessScope scope, List<String> reviewReasons) {
        return new PlacePetPolicyClassifier.Classification(analysis(scope), reviewReasons, List.of());
    }

    private PetPolicyAnalysis analysis(AccessScope scope) {
        return new PetPolicyAnalysis(
            1L,
            scope,
            null,
            null,
            null,
            null,
            WeightLimitType.UNKNOWN,
            null,
            null,
            null,
            null,
            null,
            null
        );
    }
}
