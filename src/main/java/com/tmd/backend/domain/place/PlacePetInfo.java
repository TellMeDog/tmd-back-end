package com.tmd.backend.domain.place;

import com.tmd.backend.external.TourApiPetInfoItem;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PlacePetInfo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "place_id")
    private Place place;


    private String acmpyTypeCd; // 동반유형코드(동반구분) ex. 전구역 동반가능
    private String acmpyPsblCpam; // 동반가능동물 ex. 전 견종 출입 가능 (맹견의 경우, 입마개 착용 필수)
    private String acmpyNeedMtr; // 동반시 필요사항 ex. 목줄 착용
    private String relaAcdntRiskMtr; // 관련 사고 대비 사항
    private String relaPosesFclty; // 관련 구비 시설
    private String relaFrnshPrdlst; // 관련 비치 품목
    private String relaPurcPrdlst; // 관련 구매 품목
    private String relaRntlPrdlst; // 관련 렌탈 품목
    private String etcAcmpyInfo; // 기타 동반 정보 ex. 안전을 위해 목줄은 2m 이내로 유지, 별도의 쓰레기 처리 시설이 없으므로 배변봉투 지참 및 수거 필수

    @Builder
    private PlacePetInfo(Place place, String acmpyTypeCd, String acmpyPsblCpam, String acmpyNeedMtr, String relaAcdntRiskMtr, String relaPosesFclty, String relaFrnshPrdlst, String relaPurcPrdlst, String relaRntlPrdlst, String etcAcmpyInfo) {
        this.place = place;
        this.acmpyTypeCd = acmpyTypeCd;
        this.acmpyPsblCpam = acmpyPsblCpam;
        this.acmpyNeedMtr = acmpyNeedMtr;
        this.relaAcdntRiskMtr = relaAcdntRiskMtr;
        this.relaPosesFclty = relaPosesFclty;
        this.relaFrnshPrdlst = relaFrnshPrdlst;
        this.relaPurcPrdlst = relaPurcPrdlst;
        this.relaRntlPrdlst = relaRntlPrdlst;
        this.etcAcmpyInfo = etcAcmpyInfo;
    }

    // 원본 응답 DTO -> PlacePetInfo로 가공
    public static PlacePetInfo from(Place place, TourApiPetInfoItem item){
        return PlacePetInfo.builder()
            .place(place)
            .acmpyTypeCd(item.getAcmpyTypeCd())
            .acmpyPsblCpam(item.getAcmpyPsblCpam())
            .acmpyNeedMtr(item.getAcmpyNeedMtr())
            .relaAcdntRiskMtr(item.getRelaAcdntRiskMtr())
            .relaPosesFclty(item.getRelaPosesFclty())
            .relaFrnshPrdlst(item.getRelaFrnshPrdlst())
            .relaPurcPrdlst(item.getRelaPurcPrdlst())
            .relaRntlPrdlst(item.getRelaRntlPrdlst())
            .etcAcmpyInfo(item.getEtcAcmpyInfo())
            .build();
    }

    // 마커색/체크리스트 계산 시 쓸, 4개 필드를 합친 텍스트
    // 근데 필드가 null로 올시 GREEN으로 볼지, YELLOW로 볼지 논의 필요
    public String combinedText() {
        return String.join(" ",
            nullToEmpty(acmpyTypeCd), nullToEmpty(acmpyPsblCpam), nullToEmpty(acmpyNeedMtr), nullToEmpty(etcAcmpyInfo));
    }

    private String nullToEmpty(String s){
        return s == null ? "" : s;
    }
}
