package com.servicekit.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.EntityListeners;

@Getter
@Setter
@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
public abstract class VersionedSoftDeletableEntity extends SoftDeletableEntity {

    /**
     * Khóa lạc quan (Optimistic Locking) kết hợp xóa mềm.
     * Chuẩn cho Core Business Domain: User, Order, Product, Contract...
     */
    @Version
    @Column(name = "version")
    private Long version;

    /**
     * ID người tạo bản ghi - tích hợp với AuditorAware từ service-kit-security.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * ID người cập nhật gần nhất.
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
