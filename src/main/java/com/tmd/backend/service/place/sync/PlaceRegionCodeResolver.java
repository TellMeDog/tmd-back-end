package com.tmd.backend.service.place.sync;

import com.tmd.backend.common.Region;
import com.tmd.backend.common.RegionDetail;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.Comparator;
import java.util.Map;

@Component
public class PlaceRegionCodeResolver {
    private static final Map<String, Region> REGION_ALIASES = Map.ofEntries(
        Map.entry("광주광역시", Region.JEONNAM_GWANGJU),
        Map.entry("전라남도", Region.JEONNAM_GWANGJU),
        Map.entry("강원도", Region.GANGWON),
        Map.entry("전라북도", Region.JEONBUK)
    );

    public RegionCodes resolve(String address) {
        if (address == null || address.isBlank()) {
            return RegionCodes.EMPTY;
        }
        String normalized = address.trim().replaceAll("\\s+", " ");
        Region region = Arrays.stream(Region.values())
            .filter(candidate -> normalized.startsWith(candidate.getRegionName() + " ")
                || normalized.equals(candidate.getRegionName()))
            .findFirst()
            .orElseGet(() -> REGION_ALIASES.entrySet().stream()
                .filter(entry -> normalized.startsWith(entry.getKey() + " ") || normalized.equals(entry.getKey()))
                .map(Map.Entry::getValue)
                .findFirst()
                .orElse(null));
        if (region == null) {
            return RegionCodes.EMPTY;
        }

        RegionDetail detail = Arrays.stream(RegionDetail.values())
            .filter(candidate -> candidate.getRegion() == region)
            .filter(candidate -> candidate.getRegionName().equals(region.getRegionName())
                || normalized.contains(" " + candidate.getRegionName() + " ")
                || normalized.endsWith(" " + candidate.getRegionName()))
            .max(Comparator.comparingInt(candidate -> candidate.getRegionName().length()))
            .orElse(null);
        return new RegionCodes(region.getCode(), detail == null ? null : detail.getCode());
    }

    public record RegionCodes(String regionCode, String districtCode) {
        private static final RegionCodes EMPTY = new RegionCodes(null, null);
    }
}
