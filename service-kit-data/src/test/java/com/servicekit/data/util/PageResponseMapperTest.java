package com.servicekit.data.util;

import com.servicekit.common.response.PageResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class PageResponseMapperTest {

    @Test
    @DisplayName("Map Page<T> sang PageResponse<T> thành công")
    void testFrom_PageDirect() {
        List<String> items = List.of("Item 1", "Item 2");
        Page<String> page = new PageImpl<>(items, PageRequest.of(0, 10), 20);

        PageResponse<String> response = PageResponseMapper.from(page);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).containsExactly("Item 1", "Item 2");
        assertThat(response.getPage()).isEqualTo(0);
        assertThat(response.getSize()).isEqualTo(10);
        assertThat(response.getTotalElements()).isEqualTo(20);
        assertThat(response.getTotalPages()).isEqualTo(2);
        assertThat(response.isHasNext()).isTrue();
        assertThat(response.isHasPrevious()).isFalse();
    }

    @Test
    @DisplayName("Map Page<T> sang PageResponse<R> thông qua mapper function")
    void testFrom_WithMapperFunction() {
        List<Integer> ids = List.of(1, 2, 3);
        Page<Integer> page = new PageImpl<>(ids, PageRequest.of(0, 5), 3);

        PageResponse<String> response = PageResponseMapper.from(page, id -> "ID-" + id);

        assertThat(response.getItems()).containsExactly("ID-1", "ID-2", "ID-3");
        assertThat(response.getTotalElements()).isEqualTo(3);
    }

    @Test
    @DisplayName("Xử lý an toàn khi truyền null Page")
    void testFrom_NullPage() {
        PageResponse<String> response = PageResponseMapper.from(null);

        assertThat(response).isNotNull();
        assertThat(response.getItems()).isEmpty();
        assertThat(response.getTotalElements()).isZero();
    }
}
