package com.servicekit.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
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
public abstract class AuditableEntity extends SoftDeletableEntity {

    /**
     * ID người tạo bản ghi. Được gán tự động bởi Spring Data JPA Auditing + AuditorAware.
     * Tích hợp với service-kit-security: SpringSecurityAuditorAware đọc userId từ AuthContextHolder.
     * Nếu chưa kích hoạt @EnableJpaAuditing, trường này sẽ không được tự động gán.
     */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /**
     * ID người cập nhật gần nhất. Được cập nhật mỗi khi entity bị save/update.
     */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;
}
