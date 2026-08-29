package com.tmd.backend.domain.place;

// import 추가함
import jakarta.persistence.*;
import lombok.AccessLevel;
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
    private PlacePetInfo placePetInfo;}
