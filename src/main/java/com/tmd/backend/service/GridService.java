package com.tmd.backend.service;

import com.tmd.backend.domain.place.FetchedRegion;
import com.tmd.backend.repository.FetchedRegionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GridService {
    private static final double GRID_SIZE = 0.02; // 2km
    private final FetchedRegionRepository fetchedRegionRepository;

    // bounding box를 인수로 받아서
    // 겹치는 좌표들 리스트 반환
    public List<double[]> calculate(double swLat, double swLng, double neLat, double neLng){
        List<double[]> cells = new ArrayList<>();
        double startLat = Math.floor(swLat);
        double startLng = Math.floor(swLng);

        for(double lat = swLat; lat <= neLat; lat+=GRID_SIZE){
            for(double lng = swLng; lng <= neLng; lng+=GRID_SIZE){
                cells.add(new double[]{lat, lng});
            }
        }
        return cells;
    }

    // 긁지 않았거나 30일이 지난 격자 고르기
    public List<double[]> findUnfetchedCells(List<double[]> cells){
        return cells.stream()
            .filter(cell ->  fetchedRegionRepository
                .findByGridLatAndGridLng(cell[0], cell[1])
                .map(fetchedRegion -> fetchedRegion.isStale(Duration.ofDays(30)))
                .orElse(true))
            .toList();
    }

    @Transactional
    public void markFetched(double gridLat, double gridLng){
        FetchedRegion region = fetchedRegionRepository
            .findByGridLatAndGridLng(gridLat, gridLng)
            .orElse(FetchedRegion.builder().gridLat(gridLat).gridLng(gridLng).build());

        fetchedRegionRepository.save(region);
    }
}
