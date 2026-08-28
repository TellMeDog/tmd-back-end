package com.tmd.backend.domain.place;

import com.tmd.backend.external.TourApiPlaceItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Place {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String contentId;

    private String zipCode;
    private String addr1;
    private String addr2;
    private String title;
    private double mapX;
    private double mapY;
    private String firstImage;
    private String firstImage2;
    private String modifiedTime;
    private String lDongRegnCd; // 시,도
    private String lDongSignguCd; // 시군구
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;

    @OneToOne(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlacePetInfo placePetInfo;

    @Builder
    public Place(String contentId, String zipCode, String addr1, String addr2, String title, double mapX, double mapY, String firstImage, String firstImage2, String modifiedTime, String lDongRegnCd, String lDongSignguCd, String lclsSystm1, String lclsSystm2, String lclsSystm3) {
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

    public static Place create(String contentId, String zipCode, String addr1, String addr2, String title, double mapX, double mapY, String firstImage, String firstImage2, String modifiedTime, String lDongRegnCd, String lDongSignguCd, String lclsSystm1, String lclsSystm2, String lclsSystm3) {
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
            .zipCode(item.getZipCode())
            .addr1(item.getAddr1())
            .addr2(item.getAddr2())
            .title(item.getTitle())
            .mapX(Double.parseDouble(item.getMapx()))
            .mapY(Double.parseDouble(item.getMapy()))
            .firstImage(item.getFirstImage())
            .firstImage2(item.getFirstImage2())
            .modifiedTime(item.getModifiedTime())
            .lDongRegnCd(item.getLDongRegnCd())
            .lDongSignguCd(item.getLDongSignguCd())
            .lclsSystm1(item.getLclsSystm1())
            .lclsSystm2(item.getLclsSystm2())
            .lclsSystm3(item.getLclsSystm3())
            .build();
    }
}
