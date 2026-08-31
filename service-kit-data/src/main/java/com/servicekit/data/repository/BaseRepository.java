package com.servicekit.data.repository;

import com.servicekit.common.exception.NotFoundException;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.repository.NoRepositoryBean;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.function.Supplier;

@NoRepositoryBean
public interface BaseRepository<T, ID> extends JpaRepository<T, ID>, JpaSpecificationExecutor<T> {

    @Transactional
    void restoreById(ID id);

    @Transactional
    void softDeleteAllByIds(List<ID> ids);

    /**
     * Tránh N+1 query bằng cách fetch kèm các quan hệ qua EntityGraph
     */
    default List<T> findAllWithGraph(Specification<T> spec, String... attributePaths) {
        return findAllWithGraph(spec, Sort.unsorted(), attributePaths);
    }

    /**
     * Tránh N+1 query bằng cách fetch kèm các quan hệ qua EntityGraph có sắp xếp
     */
    List<T> findAllWithGraph(Specification<T> spec, Sort sort, String... attributePaths);

    /**
     * Dùng projection để chỉ query ra những cột cần thiết
     */
    <R> List<R> selectMany(Specification<T> spec, Class<R> projectionType);

    // TODO: Triển khai phân trang (Pagination) nâng cao tích hợp EntityGraph/Projection và Advanced Dynamic Filter

    /**
     * Tìm entity theo ID, tự động ném NotFoundException nếu không tồn tại
     */
    default T findByIdOrThrow(ID id) {
        return findById(id)
                .orElseThrow(() -> new NotFoundException(String.format("Resource with id '%s' not found", id)));
    }

    /**
     * Tìm entity theo ID với custom Exception Supplier
     */
    default T findByIdOrThrow(ID id, Supplier<? extends RuntimeException> exceptionSupplier) {
        return findById(id).orElseThrow(exceptionSupplier);
    }
}