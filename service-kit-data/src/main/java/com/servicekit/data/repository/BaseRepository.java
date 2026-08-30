package com.servicekit.data.repository;

import com.servicekit.common.exception.NotFoundException;
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