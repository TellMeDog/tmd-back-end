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

    private String zipCode;
    private String addr1;
    private String addr2;
    private String title;
    private Double mapX;
    private Double mapY;
    private String firstImage;
    private String firstImage2;
    private String modifiedTime;
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

    private static String syncVersion(String sourceModifiedTime) {
        return sourceModifiedTime == null ? UNKNOWN_SOURCE_VERSION : sourceModifiedTime;
    }
}
