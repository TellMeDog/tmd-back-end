package com.tmd.backend.external;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Getter
@NoArgsConstructor
public class TourApiResponse<T> {
    private Body<T> response;

    @Getter
    @NoArgsConstructor
    public static class Body<T>{
        private Header header;
        private ResponseBody<T> body;
    }

    @Getter
    @NoArgsConstructor
    public static class Header {
        private String resultCode;
        private String resultMsg;
    }

    @Getter
    @NoArgsConstructor
    public static class ResponseBody<T>{
        private Items<T> items;
        private Integer numOfRows;
        private Integer pageNo;
        private Integer totalCount;

        public List<T> itemList() {
            return items == null || items.getItem() == null ? List.of() : items.getItem();
        }
    }
    @Getter
    @NoArgsConstructor
    public static class Items<T>{
        private List<T> item = new ArrayList<>(); // Jackson3 NPE 방지
    }

    public ResponseBody<T> getBody(){
        return response == null ? null : response.getBody();
    }

    public Header getHeader() {
        return response == null ? null : response.getHeader();
    }
}
