package com.tmd.backend.external.animalhospital;

import org.locationtech.proj4j.CRSFactory;
import org.locationtech.proj4j.CoordinateReferenceSystem;
import org.locationtech.proj4j.CoordinateTransform;
import org.locationtech.proj4j.CoordinateTransformFactory;
import org.locationtech.proj4j.ProjCoordinate;
import org.springframework.stereotype.Component;

@Component
public class AnimalHospitalCoordinateConverter {
    private static final String EPSG_5174_PARAMETERS =
        "+proj=tmerc +lat_0=38 +lon_0=127.0028902777778 +k=1 " +
        "+x_0=200000 +y_0=500000 +ellps=bessel +units=m +no_defs " +
        "+towgs84=-115.80,474.99,674.11,1.16,-2.31,-1.63,6.43";

    private final CoordinateTransform transform;

    public AnimalHospitalCoordinateConverter() {
        CRSFactory crsFactory = new CRSFactory();
        CoordinateReferenceSystem source = crsFactory.createFromParameters("EPSG:5174", EPSG_5174_PARAMETERS);
        CoordinateReferenceSystem target = crsFactory.createFromParameters(
            "EPSG:4326", "+proj=longlat +datum=WGS84 +no_defs"
        );
        this.transform = new CoordinateTransformFactory().createTransform(source, target);
    }

    public Coordinates convert(String xValue, String yValue) {
        if (xValue == null || xValue.isBlank() || yValue == null || yValue.isBlank()) {
            throw new IllegalArgumentException("Animal hospital coordinate is missing");
        }
        try {
            ProjCoordinate result = new ProjCoordinate();
            transform.transform(
                new ProjCoordinate(Double.parseDouble(xValue.trim()), Double.parseDouble(yValue.trim())),
                result
            );
            if (!Double.isFinite(result.x) || !Double.isFinite(result.y)
                || result.x < 124 || result.x > 132 || result.y < 33 || result.y > 39) {
                throw new IllegalArgumentException("Converted coordinate is outside South Korea");
            }
            return new Coordinates(result.x, result.y);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Animal hospital coordinate is invalid", exception);
        }
    }

    public record Coordinates(double longitude, double latitude) {}
}
