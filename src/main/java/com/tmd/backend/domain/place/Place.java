package com.tmd.backend.domain.place;

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

    private String title;

    private String addr;

    private double mapX;

    private double mapY;

    private String lclsSystm1;

    private String lclsSystm2;

    private String lclsSystm3;

    @OneToOne(mappedBy = "place", cascade = CascadeType.ALL, orphanRemoval = true)
    private PlacePetInfo placePetInfo;

    @Builder
    private Place(String contentId, String title, String addr, double mapX, double mapY, String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        this.contentId = contentId;
        this.title = title;
        this.addr = addr;
        this.mapX = mapX;
        this.mapY = mapY;
        this.lclsSystm1 = lclsSystm1;
        this.lclsSystm2 = lclsSystm2;
        this.lclsSystm3 = lclsSystm3;
    }

    public static Place create(String contentId, String title, String addr, double mapX, double mapY, String lclsSystm1, String lclsSystm2, String lclsSystm3) {
        return Place.builder()
            .contentId(contentId)
            .title(title)
            .addr(addr)
            .mapX(mapX)
            .mapY(mapY)
            .lclsSystm1(lclsSystm1)
            .lclsSystm2(lclsSystm2)
            .lclsSystm3(lclsSystm3)
            .build();
    }
}
