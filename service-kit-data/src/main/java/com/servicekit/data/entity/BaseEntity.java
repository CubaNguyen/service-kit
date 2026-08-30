package com.servicekit.data.entity;

import com.servicekit.common.contract.IAuditable;
import com.servicekit.common.util.TimeUtils;
import jakarta.persistence.Column;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.UuidGenerator;

import java.io.Serializable;
import java.util.UUID;

@Getter
@Setter
@MappedSuperclass
public abstract class BaseEntity implements IAuditable, Serializable {

    /**
     * UUID v7 (Time-based) được sinh tự động bởi Hibernate UuidGenerator.
     * Lý do chọn UUID v7 thay vì AUTO_INCREMENT hay UUID v4:
     * - Vẫn an toàn trong môi trường phân tán (nhiều service, nhiều DB).
     * - Vẫn bảo mật, không lộ số thứ tự.
     * - ĐẶC BIỆT: Time-sorted (có chứa timestamp ở các bit đầu) giúp B-tree index
     *   hoạt động cực kỳ hiệu quả (tuần tự như Long), loại bỏ hoàn toàn nhược điểm
     *   phân mảnh index của UUID v4 truyền thống trên các bảng lớn.
     */
    @Id
    @GeneratedValue
    @UuidGenerator(style = UuidGenerator.Style.TIME)
    @Column(name = "id", updatable = false, nullable = false, columnDefinition = "uuid")
    private UUID id;

    @Column(name = "created_at", updatable = false, nullable = false)
    private Long createdAt;

    @Column(name = "updated_at", nullable = false)
    private Long updatedAt;

    @PrePersist
    protected void onCreate() {
        long now = TimeUtils.nowEpochMilli();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = TimeUtils.nowEpochMilli();
    }
}