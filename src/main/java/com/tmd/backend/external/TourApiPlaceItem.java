package com.tmd.backend.external;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class TourApiPlaceItem {
    private String contentid;
    private String zipcode;
    private String addr1;
    private String addr2;
    private String title;
    private String mapx; // 경도(Lng)
    private String mapy; // 위도(Lat)
    private String firstimage;
    private String firstimage2;
    private String modifiedtime; // 최근 수정일
    private String lDongRegnCd; // 시,도
    private String lDongSignguCd; // 시군구
    private String lclsSystm1;
    private String lclsSystm2;
    private String lclsSystm3;
}
