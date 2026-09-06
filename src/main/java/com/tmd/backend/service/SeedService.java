package com.tmd.backend.service;

import com.tmd.backend.ai.PetPolicyAnalysis;
import com.tmd.backend.ai.PetPolicyAnalyzer;
import com.tmd.backend.common.PetInfoStatus;
import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlacePetInfo;
import com.tmd.backend.domain.place.PlacePetPolicy;
import com.tmd.backend.external.TourApiClient;
import com.tmd.backend.external.TourApiPetInfoItem;
import com.tmd.backend.repository.PetPolicyRepository;
import com.tmd.backend.repository.PlacePetInfoRepository;
import com.tmd.backend.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {
    private final TourApiClient tourApiClient;
    private final PlaceRepository placeRepository;
    private final PetPolicyAnalyzer petPolicyAnalyzer;
    private final PlacePetInfoRepository placePetInfoRepository;
    private final PetPolicyRepository petPolicyRepository;

    private static final int ANALYSIS_BATCH_SIZE = 1;
    private static final int DAILY_API_LIMIT = 500;
    private static final int BATCH_SIZE = 50;

    @Async
    public void seedAllPlaces() {
        log.info("Place 시딩 시작");
        List<Place> allPlaces = tourApiClient.getTourSyncList().stream()
            .map(Place::from)
            .toList();
        log.info("전국 Place 개수: {}", allPlaces.size());
        placeRepository.saveAll(allPlaces);
        log.info("Place 시딩 완료");
    }

    public void fetchPetPolicies() {
        log.info("PlacePetInfo 수집 시작");

        int totalApiCallCount = 0;
        int totalSavedCount = 0;
        boolean limitExceeded = false;

        while (totalApiCallCount < DAILY_API_LIMIT) {

            int remaining = DAILY_API_LIMIT - totalApiCallCount;
            int batchSize = Math.min(BATCH_SIZE, remaining);

            List<Place> targets =
                placeRepository.findPlacesWithoutPetInfo(batchSize);

            if (targets.isEmpty()) {
                log.info("모든 Place의 PetInfo 수집 완료");
                break;
            }

            List<PlacePetInfo> petInfoList = new ArrayList<>();

            for (Place place : targets) {

                if (totalApiCallCount >= DAILY_API_LIMIT) {
                    break;
                }

                totalApiCallCount++;

                try {
                    TourApiPetInfoItem item =
                        tourApiClient.getPetTourInfo(place.getContentId());

                    if (item != null) {
                        petInfoList.add(
                            PlacePetInfo.from(place, item, PetInfoStatus.SUCCESS)
                        );
                    }
                    else{
                        petInfoList.add(
                            PlacePetInfo.empty(place, PetInfoStatus.NO_DATA)
                        );
                    }

                } catch (HttpClientErrorException.TooManyRequests e) {
                    limitExceeded = true;

                    log.warn(
                        "일일 API 호출 한도 초과 - 현재까지 API 호출: {}회",
                        totalApiCallCount
                    );

                    break;
                }
            }

            // 429가 발생했더라도 지금까지 성공한 데이터는 저장
            if (!petInfoList.isEmpty()) {
                placePetInfoRepository.saveAll(petInfoList);
                totalSavedCount += petInfoList.size();
            }

            log.info(
                "배치 완료 - 대상: {}건 / API 호출: {}회 / 저장: {}건 / 누적 호출: {}회 / 누적 저장: {}건",
                targets.size(),
                targets.size(),
                petInfoList.size(),
                totalApiCallCount,
                totalSavedCount
            );

            if (limitExceeded) {
                log.warn("일일 API 호출 한도 초과로 수집을 종료합니다.");
                break;
            }
        }
        log.info(
            "PlacePetInfo 수집 종료 - 총 API 호출: {}회 / 총 저장: {}건",
            totalApiCallCount,
            totalSavedCount
        );
    }

    @Async
    public void analyzePetPolicies() {
        log.info("정책 분석 시작 (배치 크기: {})", ANALYSIS_BATCH_SIZE);
        int totalProcessed = 0;

        while (true) {
            List<PlacePetInfo> batch =
                placePetInfoRepository.findUnprocessed(PageRequest.of(0, ANALYSIS_BATCH_SIZE));

            if (batch.isEmpty()) {
                log.info("모든 정책 분석 완료. 총 처리: {}건", totalProcessed);
                break;
            }

            try {

                long start = System.currentTimeMillis();

                log.info("DeepSeek 분석 요청 시작 - batch size: {}", batch.size());
                List<PetPolicyAnalysis> analyses = petPolicyAnalyzer.analyze(batch);

                long elapsed = System.currentTimeMillis() - start;

                log.info(
                    "DeepSeek 분석 완료 - {}ms / 결과: {}건",
                    elapsed,
                    analyses.size()
                );

                // 2. 응답 자체 검증
                if (analyses == null || analyses.isEmpty()) {
                    log.warn("DeepSeek 응답이 비어있습니다.");
                    break;
                }

                // 3. 요청/응답 개수 검증
                if (analyses.size() != batch.size()) {
                    log.warn(
                        "분석 결과 개수 불일치 - 요청: {}건 / 응답: {}건",
                        batch.size(),
                        analyses.size()
                    );
                    break;
                }

                // 4. placeId 검증
                if (analyses.stream().anyMatch(a -> a.placeId() == null)) {
                    log.warn("placeId가 null인 분석 결과가 존재합니다.");
                    break;
                }

                // 5. Entity 변환
                List<PlacePetPolicy> policies = analyses.stream()
                    .map(analysis -> {
                        Place place = placeRepository.findById(analysis.placeId())
                            .orElseThrow(() ->
                                new IllegalStateException(
                                    "placeId 없음: " + analysis.placeId()
                                )
                            );

                        return PlacePetPolicy.from(place, analysis);
                    })
                    .toList();

                // 6. 검증이 모두 끝난 후 저장
                petPolicyRepository.saveAll(policies);

                totalProcessed += policies.size();

                log.info(
                    "배치 저장 완료 - {}건 / 누적 {}건",
                    policies.size(),
                    totalProcessed
                );

            } catch (Exception e) {
                log.error(
                    "정책 분석 중 오류 발생 - 현재까지 처리: {}건",
                    totalProcessed,
                    e
                );
                break;
            }
        }
    }
}
