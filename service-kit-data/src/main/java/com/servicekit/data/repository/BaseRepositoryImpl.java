package com.servicekit.data.repository;

import com.servicekit.data.entity.SoftDeletableEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Table;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public void delete(T entity) {
        if (entity instanceof SoftDeletableEntity softDeletable) {
            softDeletable.setIsDeleted(true);
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
        String jpql = String.format("UPDATE %s e SET e.isDeleted = true, e.updatedAt = :now WHERE e.id IN :ids", entityName);
        entityManager.createQuery(jpql)
                .setParameter("now", System.currentTimeMillis())
                .setParameter("ids", ids)
                .executeUpdate();

        // Xóa sạch first-level cache để các câu query findById sau đó buộc phải đọc từ DB
        entityManager.clear();
    }

    @Override
    @Transactional
    public void restoreById(ID id) {
        // Đẩy các thay đổi pending trước khi chạy native SQL
        entityManager.flush();

        Class<T> domainClass = entityInformation.getJavaType();
        String tableName = domainClass.isAnnotationPresent(Table.class) && !domainClass.getAnnotation(Table.class).name().isBlank()
                ? domainClass.getAnnotation(Table.class).name()
                : entityInformation.getEntityName();

        // Native SQL là bắt buộc để bypass @SQLRestriction("is_deleted = false") của Hibernate
        // UUID phải cast về ::uuid trên PostgreSQL, nhưng đây dùng parameterized query nên driver tự xử lý
        String sql = String.format("UPDATE %s SET is_deleted = false, updated_at = :now WHERE id = :id", tableName);

        entityManager.createNativeQuery(sql)
                .setParameter("now", System.currentTimeMillis())
                .setParameter("id", id instanceof UUID ? id.toString() : id)
                .executeUpdate();

        // Xóa sạch first-level cache
        entityManager.clear();
    }
}