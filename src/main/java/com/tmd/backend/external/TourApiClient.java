package com.tmd.backend.external;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class TourApiClient {
    private static final String BASE_URL = "https://apis.data.go.kr/B551011/KorPetTourService2";
    private static final long NUM_OF_ROWS = 11000;

    @Value("${tourapi.service-key}")
    private String serviceKey;

    private final RestClient restClient = RestClient.create();

    public TourApiPetInfoItem getPetTourInfo(String contentId) {
        URI uri = UriComponentsBuilder.newInstance()
            .path(BASE_URL + "/detailPetTour2")
            .query("serviceKey={serviceKey}")
            .query("MobileOS=ETC")
            .query("MobileApp=TellMeDog")
            .query("contentId={contentId}")
            .query("_type=json")
            .buildAndExpand(serviceKey, contentId)
            .toUri();

        TourApiResponse<TourApiPetInfoItem> response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(new ParameterizedTypeReference<TourApiResponse<TourApiPetInfoItem>>() {});

        if (response == null || response.getResponse().getBody() == null) {
            return null;
        }
        List<TourApiPetInfoItem> items = response.getBody().getItems().getItem();
        return items.isEmpty() ? null : items.get(0);
    }

    public List<TourApiPlaceItem> getLocationBasedListByCategory(
        double mapX, double mapY, String radius, String lclsSystm1, String lclsSystm2, String lclsSystm3){
        URI uri = UriComponentsBuilder
            .fromUriString(BASE_URL)
            .path("/locationBasedList2")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", NUM_OF_ROWS)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "TellMeDog")
            .queryParam("mapX", mapX)
            .queryParam("mapY", mapY)
            .queryParam("radius", radius)
            .queryParam("lclsSystm1", lclsSystm1)
            .queryParamIfPresent("lclsSystm2", Optional.ofNullable(lclsSystm2))
            .queryParamIfPresent("lclsSystm3", Optional.ofNullable(lclsSystm3))
            .queryParam("_type", "json")
            .build()
            .toUri();

        TourApiResponse<TourApiPlaceItem> response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(new ParameterizedTypeReference<TourApiResponse<TourApiPlaceItem>>() {});

        if (response == null || response.getResponse().getBody() == null) {
            log.warn("TourAPI 응답 없음: mapX={}, mapY={}", mapX, mapY);
            return List.of();
        }
        return response.getBody().getItems().getItem();
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
        URI uri = UriComponentsBuilder
            .fromUriString(BASE_URL)
            .path("/ldongCode2")
            .queryParam("serviceKey", serviceKey)
            .queryParam("numOfRows", NUM_OF_ROWS)
            .queryParam("MobileOS", "ETC")
            .queryParam("MobileApp", "TellMeDog")
            .queryParam("_type", "json")
            .encode()
            .build()
            .toUri();

        TourApiResponse<TourApiPlaceItem> response = restClient.get()
            .uri(uri)
            .retrieve()
            .body(new ParameterizedTypeReference<TourApiResponse<TourApiPlaceItem>>() {
            });

        if(response==null || response.getResponse().getBody() == null){
            log.warn("TourAPI 응답 없음");
            return List.of();
        }
        return response.getBody().getItems().getItem();
    }
}
