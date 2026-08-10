package com.tmd.backend.dto.response;

import lombok.Getter;

import java.util.List;

@Getter
public class PageResponse<T> {
    private final List<T> content;
    private final int totalElements;
    private final int totalPages;

    public PageResponse(List<T> content, int totalElements, int totalPages) {
        this.content = content;
        this.totalElements = totalElements;
        this.totalPages = totalPages;
    }
}
