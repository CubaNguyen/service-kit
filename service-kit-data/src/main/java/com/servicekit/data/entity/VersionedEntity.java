package com.servicekit.data.entity;

import jakarta.persistence.Column;
import jakarta.persistence.MappedSuperclass;
import jakarta.persistence.Version;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@MappedSuperclass
public abstract class VersionedEntity extends BaseEntity {

    /**
     * Khóa lạc quan (Optimistic Locking) chống race condition và lost updates
     */
    @Version
    @Column(name = "version")
    private Long version;
}
