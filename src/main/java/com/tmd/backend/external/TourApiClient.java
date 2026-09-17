package com.tmd.backend.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.time.Duration;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
public class TourApiClient {
    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorPetTourService2";
    private static final int NUM_OF_ROWS = 11000;

    @Value("${tourapi.service-key}")
    private String serviceKey;

    @Value("${tourapi.sync.page-size:11000}")
    private int syncPageSize;

    @Value("${tourapi.sync.arrange:}")
    private String syncArrange;

    private final RestClient restClient;

    public TourApiClient(
        RestClient.Builder builder,
        @Value("${tourapi.connect-timeout:10s}") Duration connectTimeout,
        @Value("${tourapi.read-timeout:180s}") Duration readTimeout
    ) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(connectTimeout);
        requestFactory.setReadTimeout(readTimeout);
        this.restClient = builder.requestFactory(requestFactory).build();
    }

    public TourApiPetInfoItem getPetTourInfo(String contentId) {
        URI uri = UriComponentsBuilder
            .fromUriString(BASE_URL)
            .path("/detailPetTour2")
            .queryParam("serviceKey", serviceKey)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "TellMeDog")
            .queryParam("contentId", contentId)
            .queryParam("_type", "json")
            .build()
            .toUri();

        TourApiResponse<TourApiPetInfoItem> response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(new ParameterizedTypeReference<TourApiResponse<TourApiPetInfoItem>>() {});

        if (response == null || response.getBody() == null) {
            return null;
        }
        List<TourApiPetInfoItem> items = response.getBody().itemList();
        return items.isEmpty() ? null : items.get(0);
    }

    public List<TourApiPlaceItem> getPlacesByKeyword(String keyword){
        URI uri = UriComponentsBuilder
            .fromUriString(BASE_URL)
            .path("/searchKeyword2")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", NUM_OF_ROWS)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "TellMeDog")
            .queryParam("keyword", keyword)
            .queryParam("_type", "json")
            .encode()
            .build()
            .toUri();

        TourApiResponse<TourApiPlaceItem> response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(new ParameterizedTypeReference<TourApiResponse<TourApiPlaceItem>>() {});

        if (response == null || response.getResponse().getBody() == null) {
            log.warn("TourAPI 응답 없음: {}", keyword);
            return List.of();
        }
        return response.getBody().getItems().getItem();
    }

    public List<TourApiPlaceItem> getTourSyncList(){
        TourApiResponse.ResponseBody<TourApiPlaceItem> firstPage = getTourSyncPage(1);
        int totalCount = firstPage.getTotalCount() == null ? 0 : firstPage.getTotalCount();
        List<TourApiPlaceItem> result = new java.util.ArrayList<>(firstPage.itemList());

        for (int page = 2; result.size() < totalCount; page++) {
            List<TourApiPlaceItem> items = getTourSyncPage(page).itemList();
            if (items.isEmpty()) {
                throw new IllegalStateException("TourAPI Sync 응답이 totalCount보다 먼저 종료되었습니다.");
            }
            result.addAll(items);
        }
        if (result.size() != totalCount) {
            throw new IllegalStateException(
                "TourAPI Sync 응답 건수가 일치하지 않습니다. totalCount=" + totalCount + ", actual=" + result.size()
            );
        }
        return List.copyOf(result);
    }

    private TourApiResponse.ResponseBody<TourApiPlaceItem> getTourSyncPage(int pageNo) {
        URI uri = UriComponentsBuilder
            .fromUriString(BASE_URL)
            .path("/petTourSyncList2")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", syncPageSize)
            .queryParam("pageNo", pageNo)
            .queryParamIfPresent("arrange", Optional.ofNullable(syncArrange).filter(value -> !value.isBlank()))
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "TellMeDog")
            .queryParam("_type", "json")
            .build()
            .toUri();

        TourApiResponse<TourApiPlaceItem> response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(new ParameterizedTypeReference<TourApiResponse<TourApiPlaceItem>>() {
            });

        if (response == null || response.getBody() == null) {
            throw new IllegalStateException("TourAPI Sync 응답 본문이 없습니다. pageNo=" + pageNo);
        }
        log.info("TourAPI Sync 페이지 수신 완료. pageNo={}, rows={}, totalCount={}",
            pageNo, response.getBody().itemList().size(), response.getBody().getTotalCount());
        return response.getBody();
    }
}
