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
    private static final int CATEGORY_PAGE_SIZE = 1000;

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

    public List<TourApiCategoryEntry> getTourCategoryCodes() {
        List<TourApiCategoryEntry> result = new java.util.ArrayList<>();
        List<TourApiCategoryItem> level1Items = getCategoryItems(null, null);
        addCategoryEntries(result, level1Items, 1, null);

        for (TourApiCategoryItem level1 : level1Items) {
            List<TourApiCategoryItem> level2Items = getCategoryItems(level1.getCode(), null);
            addCategoryEntries(result, level2Items, 2, level1.getCode());
            for (TourApiCategoryItem level2 : level2Items) {
                List<TourApiCategoryItem> level3Items = getCategoryItems(level1.getCode(), level2.getCode());
                addCategoryEntries(result, level3Items, 3, level2.getCode());
            }
        }
        return List.copyOf(result);
    }

    private void addCategoryEntries(List<TourApiCategoryEntry> target,
                                    List<TourApiCategoryItem> items,
                                    int depth,
                                    String parentCode) {
        for (int index = 0; index < items.size(); index++) {
            TourApiCategoryItem item = items.get(index);
            int displayOrder = item.getRnum() == null ? index + 1 : item.getRnum();
            target.add(new TourApiCategoryEntry(
                item.getCode(), item.getName(), depth, parentCode, displayOrder
            ));
        }
    }

    private List<TourApiCategoryItem> getCategoryItems(String level1Code, String level2Code) {
        TourApiResponse.ResponseBody<TourApiCategoryItem> firstPage = getCategoryPage(1, level1Code, level2Code);
        int totalCount = firstPage.getTotalCount() == null ? 0 : firstPage.getTotalCount();
        List<TourApiCategoryItem> result = new java.util.ArrayList<>(firstPage.itemList());
        for (int page = 2; result.size() < totalCount; page++) {
            List<TourApiCategoryItem> items = getCategoryPage(page, level1Code, level2Code).itemList();
            if (items.isEmpty()) {
                throw new IllegalStateException("TourAPI category response ended before totalCount");
            }
            result.addAll(items);
        }
        if (result.size() != totalCount) {
            throw new IllegalStateException(
                "TourAPI category response count mismatch. totalCount=" + totalCount + ", actual=" + result.size()
            );
        }
        return List.copyOf(result);
    }

    private TourApiResponse.ResponseBody<TourApiCategoryItem> getCategoryPage(
        int pageNo,
        String level1Code,
        String level2Code
    ) {
        URI uri = UriComponentsBuilder
            .fromUriString(BASE_URL)
            .path("/lclsSystmCode2")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", CATEGORY_PAGE_SIZE)
            .queryParam("pageNo", pageNo)
            .queryParamIfPresent("lclsSystm1", Optional.ofNullable(level1Code))
            .queryParamIfPresent("lclsSystm2", Optional.ofNullable(level2Code))
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "TellMeDog")
            .queryParam("_type", "json")
            .build()
            .toUri();

        TourApiResponse<TourApiCategoryItem> response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(new ParameterizedTypeReference<TourApiResponse<TourApiCategoryItem>>() {
            });
        if (response == null || response.getHeader() == null
            || !"0000".equals(response.getHeader().getResultCode())) {
            String message = response == null || response.getHeader() == null
                ? "missing response header"
                : response.getHeader().getResultCode() + ": " + response.getHeader().getResultMsg();
            throw new IllegalStateException("TourAPI category request failed: " + message);
        }
        if (response.getBody() == null) {
            throw new IllegalStateException("TourAPI category response body is missing. pageNo=" + pageNo);
        }
        return response.getBody();
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
