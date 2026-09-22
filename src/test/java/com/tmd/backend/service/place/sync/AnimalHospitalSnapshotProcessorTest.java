package com.tmd.backend.service.place.sync;

import com.tmd.backend.domain.place.Place;
import com.tmd.backend.domain.place.PlaceSource;
import com.tmd.backend.external.animalhospital.AnimalHospitalCoordinateConverter;
import com.tmd.backend.external.animalhospital.AnimalHospitalItem;
import com.tmd.backend.repository.place.PlaceRepository;
import com.tmd.backend.repository.place.TourCategoryRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

class AnimalHospitalSnapshotProcessorTest {
    private final PlaceRepository placeRepository = mock(PlaceRepository.class);
    private final TourCategoryRepository categoryRepository = mock(TourCategoryRepository.class);
    private final AnimalHospitalCoordinateConverter converter = mock(AnimalHospitalCoordinateConverter.class);
    private final PlaceRegionCodeResolver regionResolver = mock(PlaceRegionCodeResolver.class);
    private final AnimalHospitalSnapshotProcessor processor = new AnimalHospitalSnapshotProcessor(
        placeRepository, categoryRepository, converter, regionResolver
    );

    @Test
    void createsOpenHospitalAndSystemCategory() {
        AnimalHospitalItem item = item("hospital-1", "01", "0000", "동물병원");
        when(placeRepository.findAllAnimalHospitalsForSync()).thenReturn(List.of());
        when(categoryRepository.findById(AnimalHospitalSnapshotProcessor.CATEGORY_CODE))
            .thenReturn(Optional.empty());
        when(converter.convert("198787", "442666"))
            .thenReturn(new AnimalHospitalCoordinateConverter.Coordinates(126.99, 37.48));
        when(regionResolver.resolve("서울특별시 서초구 서초대로 43"))
            .thenReturn(new PlaceRegionCodeResolver.RegionCodes("11", "650"));

        AnimalHospitalSyncSummary result = processor.apply(List.of(item));

        assertThat(result).isEqualTo(new AnimalHospitalSyncSummary(1, 1, 1, 0, 0, 0));
        ArgumentCaptor<Place> placeCaptor = ArgumentCaptor.forClass(Place.class);
        verify(placeRepository).save(placeCaptor.capture());
        Place saved = placeCaptor.getValue();
        assertThat(saved.getContentId()).isEqualTo("MOIS-HOSPITAL:hospital-1");
        assertThat(saved.getSource()).isEqualTo(PlaceSource.ANIMAL_HOSPITAL);
        assertThat(saved.getLclsSystm1()).isEqualTo("TMDHOSP");
        assertThat(saved.getTel()).isEqualTo("02-123-4567");
        verify(categoryRepository).save(argThat(category ->
            category.getCode().equals("TMDHOSP") && !category.isTourApiManaged()
        ));
    }

    @Test
    void incompleteDuplicateSnapshotChangesNothing() {
        AnimalHospitalItem duplicate = item("hospital-1", "01", "0000", "동물병원");

        assertThatThrownBy(() -> processor.apply(List.of(duplicate, duplicate)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessageContaining("duplicate");
        verifyNoInteractions(placeRepository, categoryRepository, converter, regionResolver);
    }

    private AnimalHospitalItem item(String id, String statusCode, String detailStatusCode, String name) {
        return new AnimalHospitalItem(
            name, "198787", "442666", "2026-09-19 22:02:00",
            detailStatusCode, "정상", "2026-09-18 18:11:03", "",
            "서울특별시 서초구 방배동", id,
            "서울특별시 서초구 서초대로 43", "06570",
            statusCode, "영업/정상", "02-123-4567"
        );
    }
}
