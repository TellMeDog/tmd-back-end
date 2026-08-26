package com.tmd.backend.external;

import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class TourApiResponse<T> {
    private Body<T> response;

    @Getter
    @NoArgsConstructor
    public static class Body<T>{
        private ResponseBody<T> body;
    }

    @Getter
    @NoArgsConstructor
    public static class ResponseBody<T>{
        private Items<T> items;
    }
    @Getter
    @NoArgsConstructor
    public static class Items<T>{
        private List<T> item;
    }

    public ResponseBody<T> getBody(){
        return response.getBody();
    }
}
