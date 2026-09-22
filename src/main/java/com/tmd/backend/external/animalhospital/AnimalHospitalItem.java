package com.tmd.backend.external.animalhospital;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalHospitalItem(
    @JsonProperty("BPLC_NM") String name,
    @JsonProperty("CRD_INFO_X") String coordinateX,
    @JsonProperty("CRD_INFO_Y") String coordinateY,
    @JsonProperty("DAT_UPDT_PNT") String dataUpdatedAt,
    @JsonProperty("DTL_SALS_STTS_CD") String detailStatusCode,
    @JsonProperty("DTL_SALS_STTS_NM") String detailStatusName,
    @JsonProperty("LAST_MDFCN_PNT") String lastModifiedAt,
    @JsonProperty("LCTN_ZIP") String locationZipCode,
    @JsonProperty("LOTNO_ADDR") String lotNumberAddress,
    @JsonProperty("MNG_NO") String managementNumber,
    @JsonProperty("ROAD_NM_ADDR") String roadAddress,
    @JsonProperty("ROAD_NM_ZIP") String roadZipCode,
    @JsonProperty("SALS_STTS_CD") String statusCode,
    @JsonProperty("SALS_STTS_NM") String statusName,
    @JsonProperty("TELNO") String telephone
) {
    public boolean isOpen() {
        return "01".equals(statusCode) && "0000".equals(detailStatusCode);
    }
}
