package com.servicekit.multitenant.entity;

import com.servicekit.data.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.TenantId;

@Getter
@Setter
@MappedSuperclass
public abstract class TenantEntity extends SoftDeletableEntity {

    /**
     * Tự động lọc và gán Tenant ID/Site Key trong suốt qua Hibernate 6 @TenantId
     */
    @TenantId
    @Column(name = "tenant_id", nullable = false, updatable = false)
    private String tenantId;
}
