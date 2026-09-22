package com.tmd.backend.external.animalhospital;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalHospitalApiResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
        String dataType,
        Items items,
        Integer numOfRows,
        Integer pageNo,
        Integer totalCount
    ) {
        public List<AnimalHospitalItem> itemList() {
            return items == null || items.item() == null ? List.of() : items.item();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<AnimalHospitalItem> item) {}
}
