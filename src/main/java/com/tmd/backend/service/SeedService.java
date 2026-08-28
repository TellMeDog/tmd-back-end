package com.tmd.backend.service;

import com.tmd.backend.domain.place.Place;
import com.tmd.backend.external.TourApiClient;
import com.tmd.backend.repository.PlaceRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class SeedService {
    private final TourApiClient tourApiClient;
    private final PlaceRepository placeRepository;

    @Async
    public void seedAllPlaces(){
        log.info("시딩 시작");

        List<Place> allPlaces = tourApiClient.getTourSyncList().stream()
            .map(Place::from)
            .toList();

        log.info("전국 시딩 개수 : {}", allPlaces.size());

        placeRepository.saveAll(allPlaces);
    }
}
