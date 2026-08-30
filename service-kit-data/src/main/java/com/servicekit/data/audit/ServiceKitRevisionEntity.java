package com.servicekit.data.audit;

import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Custom Revision Entity mở rộng Hibernate Envers mặc định.
 *
 * Ngoài revision_id và revision_timestamp mặc định, bổ sung thêm:
 * - modifiedBy: ID của người dùng đang thực hiện thay đổi (đọc từ context)
 *
 * Tích hợp với service-kit-security: RevisionListener sẽ đọc userId
 * từ AuthContextHolder hoặc SecurityContextHolder.
 *
 * Cách bật: Thêm @EnableEnvers vào class Application và khai báo Listener.
 */
@Getter
@Setter
@Entity
@Table(name = "revinfo")
@RevisionEntity(ServiceKitRevisionListener.class)
public class ServiceKitRevisionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.SEQUENCE, generator = "revinfo_seq")
    @RevisionNumber
    @Column(name = "rev")
    private int rev;

    @RevisionTimestamp
    @Column(name = "revtstmp")
    private long revtstmp;

    /**
     * ID người thực hiện thay đổi.
     * Được gán tự động bởi ServiceKitRevisionListener.
     * Null nếu thay đổi đến từ System/Background Job.
     */
    @Column(name = "modified_by")
    private String modifiedBy;
}
