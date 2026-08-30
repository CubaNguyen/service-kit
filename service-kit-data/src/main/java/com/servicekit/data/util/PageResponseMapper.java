package com.servicekit.data.util;

import com.servicekit.common.response.PageResponse;
import org.springframework.data.domain.Page;

import java.util.List;
import java.util.function.Function;

public final class PageResponseMapper {

    private PageResponseMapper() {}

    /**
     * Map Page<T> sang PageResponse<T> giữ nguyên kiểu dữ liệu
     */
    public static <T> PageResponse<T> from(Page<T> page) {
        if (page == null) {
            return PageResponse.of(List.of(), 0, 0, 0);
        }
        return PageResponse.of(
                page.getContent(),
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }

    /**
     * Map Page<T> (Entity) sang PageResponse<R> (DTO) thông qua hàm chuyển đổi mapper
     */
    public static <T, R> PageResponse<R> from(Page<T> page, Function<T, R> mapper) {
        if (page == null) {
            return PageResponse.of(List.of(), 0, 0, 0);
        }
        List<R> mappedItems = page.getContent().stream().map(mapper).toList();
        return PageResponse.of(
                mappedItems,
                page.getNumber(),
                page.getSize(),
                page.getTotalElements()
        );
    }
}