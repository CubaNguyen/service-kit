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
     * UUID v4 (Random) được sinh tự động bởi Hibernate UuidGenerator.
     * Lý do chọn UUID thay vì AUTO_INCREMENT:
     * - An toàn trong môi trường phân tán (nhiều service, nhiều DB, Event Sourcing)
     * - Không lộ số thứ tự bản ghi qua API (bảo mật)
     * - Không bị collision khi merge/sync dữ liệu giữa các DB
     * - Tương thích với pattern Outbox, Saga, CQRS
     * Trade-off: Index lớn hơn, cần cân nhắc dùng UUID v7 (ULID) để tăng hiệu năng B-tree index khi scale lớn.
     */
    @Id
    @GeneratedValue
    @UuidGenerator
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