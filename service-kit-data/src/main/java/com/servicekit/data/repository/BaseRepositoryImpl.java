package com.servicekit.data.repository;

import com.servicekit.common.util.TimeUtils;
import com.servicekit.data.entity.SoftDeletableEntity;
import jakarta.persistence.EntityGraph;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.Table;

import java.io.Serializable;
import java.util.List;
import java.util.UUID;

public class BaseRepositoryImpl<T, ID extends Serializable>
        extends SimpleJpaRepository<T, ID>
        implements BaseRepository<T, ID> {

    private final JpaEntityInformation<T, ?> entityInformation;
    private final EntityManager entityManager;

    public BaseRepositoryImpl(JpaEntityInformation<T, ?> entityInformation, EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityInformation = entityInformation;
        this.entityManager = entityManager;
    }

    // =========================================================================
    // XX  QUERY KERNEL
    // =========================================================================

    /**
     * Trái tim của mọi read operation. Chịu trách nhiệm duy nhất việc dựng TypedQuery<T>:
     * - Áp dụng Specification (filter + distinct)
     * - Áp dụng Sort
     * - Gắn EntityGraph nếu có (tránh N+1)
     *
     * Mọi public method đọc dữ liệu PHẢI tái sử dụng method này — không tự dựng query riêng.
     * Pagination và AdvancedFilter (TODO) sẽ đều gọi xuống đây.
     */
    protected TypedQuery<T> buildQuery(Specification<T> spec, Sort sort, EntityGraph<T> entityGraph) {
        TypedQuery<T> query = getQuery(spec, sort != null ? sort : Sort.unsorted());
        if (entityGraph != null) {
            query.setHint("jakarta.persistence.fetchgraph", entityGraph);
        }
        return query;
    }

    /**
     * Overload không có EntityGraph — dùng cho các case không cần eager load.
     */
    protected TypedQuery<T> buildQuery(Specification<T> spec, Sort sort) {
        return buildQuery(spec, sort, null);
    }

    /**
     * Helper: bọc Specification gốc để tự động đặt distinct(true).
     * Nên dùng khi query có khả năng JOIN/FETCH collection
     * (@OneToMany / @ManyToMany) để tránh duplicate root entity.
     * EntityGraph tự nó không gây duplicate — nguyên nhân là do SQL JOIN
     * với collection sinh ra nhiều row hơn số root entity thực sự.
     */
    protected Specification<T> withDistinct(Specification<T> spec) {
        return (root, query, cb) -> {
            query.distinct(true);
            return spec != null ? spec.toPredicate(root, query, cb) : null;
        };
    }

    /**
     * Helper: dựng EntityGraph động từ danh sách attribute path.
     * Trả về null nếu không có path nào để buildQuery() bỏ qua hint.
     */
    protected EntityGraph<T> buildEntityGraph(String... attributePaths) {
        if (attributePaths == null || attributePaths.length == 0) {
            return null;
        }
        EntityGraph<T> graph = entityManager.createEntityGraph(entityInformation.getJavaType());
        for (String path : attributePaths) {
            graph.addAttributeNodes(path);
        }
        return graph;
    }

    // =========================================================================
    // XX  PUBLIC READ OPERATIONS  (tái sử dụng buildQuery — không tự dựng query)
    // =========================================================================

    @Override
    public List<T> findAllWithGraph(Specification<T> spec, Sort sort, String... attributePaths) {
        // Dùng withDistinct() để tránh duplicate root entity khi SQL JOIN với collection
        return buildQuery(withDistinct(spec), sort, buildEntityGraph(attributePaths))
                .getResultList();
    }

    @Override
    public <R> List<R> selectMany(Specification<T> spec, Class<R> projectionType) {
        return findBy(spec, q -> q.as(projectionType).all());
    }

    // TODO: Pagination & AdvancedFilter — sẽ tái sử dụng buildQuery() và getCountQuery() làm nền tảng

    // =========================================================================
    // XX  LIFECYCLE — SOFT DELETE & RESTORE
    // =========================================================================

    @Override
    @Transactional
    public void delete(T entity) {
        if (entity instanceof SoftDeletableEntity softDeletable) {
            softDeletable.setIsDeleted(true);
            softDeletable.setDeletedAt(TimeUtils.nowEpochMilli());
            super.save(entity);
        } else {
            super.delete(entity);
        }
    }

    @Override
    @Transactional
    public void deleteById(ID id) {
        findById(id).ifPresent(this::delete);
    }

    @Override
    @Transactional
    public void deleteAll(Iterable<? extends T> entities) {
        if (entities != null) {
            entities.forEach(this::delete);
        }
    }

    @Override
    @Transactional
    public void deleteAllById(Iterable<? extends ID> ids) {
        if (ids != null) {
            ids.forEach(this::deleteById);
        }
    }

    @Override
    @Transactional
    public void softDeleteAllByIds(List<ID> ids) {
        if (ids == null || ids.isEmpty()) {
            return;
        }

        // Đẩy toàn bộ thay đổi đang pending trong Persistence Context xuống DB trước khi execute bulk query
        entityManager.flush();

        String entityName = entityInformation.getEntityName();
        String jpql = String.format(
                "UPDATE %s e SET e.isDeleted = true, e.updatedAt = :now, e.deletedAt = :now WHERE e.id IN :ids",
                entityName
        );
        entityManager.createQuery(jpql)
                .setParameter("now", TimeUtils.nowEpochMilli())
                .setParameter("ids", ids)
                .executeUpdate();

        // Xóa sạch first-level cache để các câu query findById sau đó buộc phải đọc từ DB
        entityManager.clear();
    }

    @Override
    @Transactional
    public void restoreById(ID id) {
        // Day cac thay doi pending truoc khi chay native SQL
        entityManager.flush();

        Class<T> domainClass = entityInformation.getJavaType();
        String tableName = domainClass.isAnnotationPresent(Table.class)
                && !domainClass.getAnnotation(Table.class).name().isBlank()
                ? domainClass.getAnnotation(Table.class).name()
                : entityInformation.getEntityName();

        // Native SQL la bat buoc de bypass @SQLRestriction("is_deleted = false") cua Hibernate
        String sql = String.format(
                "UPDATE %s SET is_deleted = false, updated_at = :now, deleted_at = 0 WHERE id = :id",
                tableName
        );

        entityManager.createNativeQuery(sql)
                .setParameter("now", TimeUtils.nowEpochMilli())
                .setParameter("id", id instanceof UUID ? id.toString() : id)
                .executeUpdate();

        // Xoa sach first-level cache
        entityManager.clear();
    }
}
