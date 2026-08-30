package com.servicekit.common.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Collections;
import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {
    private List<T> items;
    private int page;
    private int size;
    private long totalElements;
    private int totalPages;
    private boolean hasNext;
    private boolean hasPrevious;

    public static <T> PageResponse<T> of(List<T> items, int page, int size, long totalElements) {
        int calculatedTotalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return PageResponse.<T>builder()
                .items(items != null ? items : Collections.emptyList())
                .page(page)
                .size(size)
                .totalElements(totalElements)
                .totalPages(calculatedTotalPages)
                .hasNext(page < calculatedTotalPages - 1)
                .hasPrevious(page > 0)
                .build();
    }
}