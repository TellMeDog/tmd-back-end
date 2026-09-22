package com.tmd.backend.external.animalhospital;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class AnimalHospitalCoordinateConverterTest {
    private final AnimalHospitalCoordinateConverter converter = new AnimalHospitalCoordinateConverter();

    @Test
    void convertsLocalDataEpsg5174CoordinateToWgs84() {
        AnimalHospitalCoordinateConverter.Coordinates result = converter.convert(
            "198787.489268795", "442666.440681077"
        );

        assertThat(result.longitude()).isBetween(126.9, 127.1);
        assertThat(result.latitude()).isBetween(37.4, 37.6);
    }

    @Test
    void rejectsMissingCoordinate() {
        assertThatThrownBy(() -> converter.convert("", null))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
