package com.tmd.backend.domain.place;

import com.tmd.backend.domain.review.Review;
import com.tmd.backend.external.tourapi.TourApiPlaceItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.math.NumberUtils;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(indexes = {
    @Index(name = "idx_place_source_active", columnList = "source, active"),
    @Index(name = "idx_place_active_lcls1", columnList = "active, lcls_systm1"),
    @Index(name = "idx_place_active_lcls2", columnList = "active, lcls_systm2"),
    @Index(name = "idx_place_active_lcls3", columnList = "active, lcls_systm3")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {
    private static final String UNKNOWN_SOURCE_VERSION = "UNKNOWN";

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String contentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32, columnDefinition = "varchar(32) default 'TOUR_API'")
    private PlaceSource source = PlaceSource.TOUR_API;

    private String zipCode;
    private String addr1;
    private String addr2;
    private String title;
    private Double mapX;
    private Double mapY;
    private String firstImage;
    private String firstImage2;
    private String modifiedTime;
    private String tel;
    private String businessStatus;
    private String petInfoSyncedModifiedTime;

    @Column(nullable = false, columnDefinition = "bit default 1")
    private Boolean active = true;
    private String lDongRegnCd; // 시,도
    private String lDongSignguCd; // 시군구
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;

    @OneToOne(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlacePetInfo placePetInfo;

    @OneToOne(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlacePetPolicy placePetPolicy;

    @Builder
    public Place(String contentId, String zipCode, String addr1, String addr2, String title, Double mapX, Double mapY, String firstImage, String firstImage2, String modifiedTime, String lDongRegnCd, String lDongSignguCd, String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        this.contentId = contentId;
        this.zipCode = zipCode;
        this.addr1 = addr1;
        this.addr2 = addr2;
        this.title = title;
        this.mapX = mapX;
        this.mapY = mapY;
        this.firstImage = firstImage;
        this.firstImage2 = firstImage2;
        this.modifiedTime = modifiedTime;
        this.lDongRegnCd = lDongRegnCd;
        this.lDongSignguCd = lDongSignguCd;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
    }

    public static Place create(String contentId, String zipCode, String addr1, String addr2, String title, Double mapX, Double mapY, String firstImage, String firstImage2, String modifiedTime, String lDongRegnCd, String lDongSignguCd, String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        return Place.builder()
            .contentId(contentId)
            .zipCode(zipCode)
            .addr1(addr1)
            .addr2(addr2)
            .title(title)
            .mapX(mapX)
            .mapY(mapY)
            .firstImage(firstImage)
            .firstImage2(firstImage2)
            .modifiedTime(modifiedTime)
            .lDongRegnCd(lDongRegnCd)
            .lDongSignguCd(lDongSignguCd)
            .lclsSystm1(lclsSystm1)
            .lclsSystm2(lclsSystm2)
            .lclsSystm3(lclsSystm3)
            .build();
    }

    public static Place from(TourApiPlaceItem item) {
        return Place.builder()
            .contentId(item.getContentid())
            .zipCode(item.getZipcode())
            .addr1(item.getAddr1())
            .addr2(item.getAddr2())
            .title(item.getTitle())
            .mapX(NumberUtils.toDouble(item.getMapx()))
            .mapY(NumberUtils.toDouble(item.getMapy()))
            .firstImage(item.getFirstimage())
            .firstImage2(item.getFirstimage2())
            .modifiedTime(item.getModifiedtime())
            .lDongRegnCd(item.getLDongRegnCd())
            .lDongSignguCd(item.getLDongSignguCd())
            .lclsSystm1(item.getLclsSystm1())
            .lclsSystm2(item.getLclsSystm2())
            .lclsSystm3(item.getLclsSystm3())
            .build();
    }

    public static Place createAnimalHospital(
        String contentId,
        String zipCode,
        String address,
        String title,
        Double longitude,
        Double latitude,
        String modifiedTime,
        String tel,
        String businessStatus,
        String regionCode,
        String districtCode,
        String categoryCode
    ) {
        Place place = Place.builder()
            .contentId(contentId)
            .zipCode(zipCode)
            .addr1(address)
            .title(title)
            .mapX(longitude)
            .mapY(latitude)
            .modifiedTime(modifiedTime)
            .lDongRegnCd(regionCode)
            .lDongSignguCd(districtCode)
            .lclsSystm1(categoryCode)
            .build();
        place.source = PlaceSource.ANIMAL_HOSPITAL;
        place.tel = tel;
        place.businessStatus = businessStatus;
        return place;
    }

    public void updateFromAnimalHospital(
        String zipCode,
        String address,
        String title,
        Double longitude,
        Double latitude,
        String modifiedTime,
        String tel,
        String businessStatus,
        String regionCode,
        String districtCode,
        String categoryCode
    ) {
        this.source = PlaceSource.ANIMAL_HOSPITAL;
        this.zipCode = zipCode;
        this.addr1 = address;
        this.addr2 = null;
        this.title = title;
        this.mapX = longitude;
        this.mapY = latitude;
        this.firstImage = null;
        this.firstImage2 = null;
        this.modifiedTime = modifiedTime;
        this.tel = tel;
        this.businessStatus = businessStatus;
        this.lDongRegnCd = regionCode;
        this.lDongSignguCd = districtCode;
        this.lclsSystm1 = categoryCode;
        this.lclsSystm2 = null;
        this.lclsSystm3 = null;
        this.active = true;
    }

    public boolean needsPetInfoSync(String sourceModifiedTime) {
        return petInfoSyncedModifiedTime == null
            || !petInfoSyncedModifiedTime.equals(syncVersion(sourceModifiedTime));
    }

    public void updateFrom(TourApiPlaceItem item) {
        this.zipCode = item.getZipcode();
        this.addr1 = item.getAddr1();
        this.addr2 = item.getAddr2();
        this.title = item.getTitle();
        this.mapX = NumberUtils.toDouble(item.getMapx());
        this.mapY = NumberUtils.toDouble(item.getMapy());
        this.firstImage = item.getFirstimage();
        this.firstImage2 = item.getFirstimage2();
        this.modifiedTime = item.getModifiedtime();
        this.lDongRegnCd = item.getLDongRegnCd();
        this.lDongSignguCd = item.getLDongSignguCd();
        this.lclsSystm1 = item.getLclsSystm1();
        this.lclsSystm2 = item.getLclsSystm2();
        this.lclsSystm3 = item.getLclsSystm3();
        this.active = !"0".equals(item.getShowflag());
    }

    public void initializePetInfoSyncVersionIfUnchanged(String previousModifiedTime) {
        if (petInfoSyncedModifiedTime == null
            && placePetInfo != null
            && java.util.Objects.equals(previousModifiedTime, modifiedTime)) {
            petInfoSyncedModifiedTime = syncVersion(modifiedTime);
        }
    }

    public void markPetInfoSynced() {
        this.petInfoSyncedModifiedTime = syncVersion(modifiedTime);
    }

    public void requestPetInfoSync() {
        this.petInfoSyncedModifiedTime = null;
    }

    public void deactivate() {
        this.active = false;
    }

    public boolean isActive() {
        return active == null || active;
    }

    public boolean isAnimalHospital() {
        return source == PlaceSource.ANIMAL_HOSPITAL;
    }

    public PlaceSource getSource() {
        return source == null ? PlaceSource.TOUR_API : source;
    }

    private static String syncVersion(String sourceModifiedTime) {
        return sourceModifiedTime == null ? UNKNOWN_SOURCE_VERSION : sourceModifiedTime;
    }
}
