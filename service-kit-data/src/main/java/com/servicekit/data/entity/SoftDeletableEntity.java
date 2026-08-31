package com.servicekit.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.SQLRestriction;

@Getter
@Setter
@MappedSuperclass
@SQLRestriction("is_deleted = false")
public abstract class SoftDeletableEntity extends BaseEntity {

    @Column(name = "is_deleted", nullable = false, columnDefinition = "boolean default false")
    private Boolean isDeleted = false;

    /**
     * Hỗ trợ MySQL đánh Composite Unique Index: (email, deleted_at).
     * BẮT BUỘC mặc định là 0 khi chưa xóa (vì MySQL cho phép nhiều giá trị NULL trong Unique Index, 
     * nếu để null sẽ không chặn được trùng lặp khi chưa xóa). 
     * Khi xóa sẽ gán = TimeUtils.nowEpochMilli().
     */
    @Column(name = "deleted_at", nullable = false, columnDefinition = "bigint default 0")
    private Long deletedAt = 0L;
}