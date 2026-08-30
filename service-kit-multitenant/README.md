# Service Kit Multi-Tenant

`service-kit-multitenant` là module cung cấp giải pháp **Đa người thuê (Multi-Tenancy)** tự động và trong suốt dành cho kiến trúc SaaS / Multi-tenant Microservices, xây dựng trên nền tảng **Java 21**, **Spring Boot 3.4.x** và **Hibernate 6.x**.

---

## 🎯 Tổng Quan Mục Đích & Roadmap Triển Khai

Module này giúp tách biệt dữ liệu giữa các khách hàng / chi nhánh / tổ chức (Tenants / Sites):
1. **Lọc dữ liệu tự động (Discriminator Column Strategy)**: Sử dụng tính năng `@TenantId` của Hibernate 6 để tự động chèn điều kiện `WHERE tenant_id = ?` vào tất cả các câu truy vấn (`findById`, `findAll`, JPQL, Criteria) mà không cần dev phải nhớ viết tay.
2. **Tự động gán Tenant ID khi INSERT**: Khi gọi `repository.save(entity)`, Hibernate tự động lấy `tenantId` từ `TenantContext` và gán vào Entity.
3. **Bóc tách Tenant ID từ HTTP Request**: Đọc các Header như `X-Tenant-Id`, `X-Site-Key`, hoặc Subdomain (ví dụ `tenant1.domain.com`) và nạp vào `TenantContextHolder` (ThreadLocal).
4. **Hỗ trợ Schema / Database per Tenant (Mở rộng nâng cao)**: Cung cấp `AbstractRoutingDataSource` hoặc `MultiTenantConnectionProvider` khi cần cô lập hoàn toàn Database vật lý giữa các khách hàng lớn.

---

## 🏗️ Cấu Trúc Thư Mục Đề Xuất (Architecture Blueprint)

```
service-kit-multitenant/
├── src/main/java/com/servicekit/multitenant/
│   ├── config/
│   │   ├── MultiTenantAutoConfiguration.java  # Auto-config đăng ký Filter và Hibernate Customizer
│   │   └── MultiTenantProperties.java         # @ConfigurationProperties("service-kit.multitenant")
│   ├── context/
│   │   └── TenantContext.java                 # ThreadLocal lưu trữ current tenantId của request
│   ├── entity/
│   │   └── TenantEntity.java                  # MappedSuperclass kế thừa SoftDeletableEntity + @TenantId
│   ├── filter/
│   │   └── TenantFilter.java                  # Servlet Filter bóc tách X-Tenant-Id / X-Site-Key từ Request
│   └── resolver/
│       └── TenantIdentifierResolver.java      # CurrentTenantIdentifierResolver kết nối Hibernate với TenantContext
└── README.md
```

---

## 🚀 Hướng Dẫn Sử Dụng (How-to-use)

### 1. Tạo Entity Thuộc Về Một Tenant

Thay vì kế thừa `SoftDeletableEntity`, Entity của bạn chỉ cần kế thừa `TenantEntity`:

```java
package com.servicekit.example.entity;

import com.servicekit.multitenant.entity.TenantEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "products")
public class ProductEntity extends TenantEntity {
    private String name;
    private Double price;
}
```

*Cột `tenant_id` đã được định nghĩa sẵn trong `TenantEntity` với annotation `@TenantId`.*

---

### 2. Sử Dụng Trong Repository & Service

Dev viết code **hoàn toàn bình thường**, không cần biết gì về `tenant_id`:

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    public Page<ProductEntity> getProducts(Pageable pageable) {
        // Tự động thêm: WHERE tenant_id = 'TENANT_A' AND is_deleted = false
        return productRepository.findAll(pageable); 
    }

    public ProductEntity createProduct(ProductDto dto) {
        ProductEntity product = new ProductEntity();
        product.setName(dto.getName());
        product.setPrice(dto.getPrice());
        
        // Tự động gán: tenant_id = 'TENANT_A', createdAt = now
        return productRepository.save(product); 
    }
}
```

---

### 3. Servlet Filter Bóc Tách `X-Tenant-Id` / `X-Site-Key`

Mẫu Filter để kích hoạt ngữ cảnh Tenant cho mỗi request:

```java
package com.servicekit.multitenant.filter;

import com.servicekit.multitenant.context.TenantContext;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Order(Ordered.HIGHEST_PRECEDENCE + 1)
public class TenantFilter extends OncePerRequestFilter {

    private static final String TENANT_HEADER = "X-Tenant-Id";
    private static final String SITE_KEY_HEADER = "X-Site-Key";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String tenantId = request.getHeader(TENANT_HEADER);
        if (tenantId == null || tenantId.isBlank()) {
            tenantId = request.getHeader(SITE_KEY_HEADER);
        }

        if (tenantId != null && !tenantId.isBlank()) {
            TenantContext.setTenantId(tenantId);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // [Bắt buộc] Xóa ThreadLocal chống rò rỉ dữ liệu giữa các request trên cùng Thread
            TenantContext.clear();
        }
    }
}
```

---

## 🗓️ TODO — Các Tính Năng Chưa Xây Dựng (Liên Quan Đến Multi-Tenancy)

> Xem chi tiết thiết kế và lý do phân tách module tại **[Root README — TODO Section](../README.md#️-todo--các-module-chưa-được-xây-dựng)**.

### TODO: Outbox Pattern Trong Môi Trường Multi-Tenant

**Vấn đề:** Khi dùng Multi-Tenancy (Discriminator Column), bảng `outbox_events` cũng cần có cột `tenant_id` để Scheduler không nhầm lẫn event của Tenant A sang Tenant B khi poll và publish lên Kafka.

```java
// TODO: OutboxEvent trong môi trường Multi-Tenant nên kế thừa TenantEntity:
@Entity
@Table(name = "outbox_events")
public class OutboxEvent extends TenantEntity {
    // tenant_id được Hibernate @TenantId tự động gán khi INSERT
    // Scheduler poll phải lọc theo tenant_id hoặc xử lý per-tenant
    private String eventType;
    private String payload;
    private boolean published = false;
}
```

### TODO: Tenant Isolation — Schema-Per-Tenant Strategy (Nâng Cao)

**Chiến lược hiện tại (đã implement):** `Discriminator Column` — tất cả Tenant trong cùng 1 DB, phân biệt nhau bằng cột `tenant_id`. Phù hợp cho **SaaS nhỏ và vừa**.

**Chiến lược nâng cao (chưa implement):** `Schema-Per-Tenant` hoặc `Database-Per-Tenant` — mỗi Tenant có Schema/DB riêng hoàn toàn. Phù hợp khi **khách hàng lớn yêu cầu cô lập dữ liệu vật lý** (bảo mật, compliance, GDPR).

```java
// TODO: Implement MultiTenantConnectionProvider cho Schema-Per-Tenant:
// Mỗi khi Hibernate cần Connection, Provider đọc TenantContext.getTenantId()
// và trả về Connection đã SET search_path = <tenant_schema> (PostgreSQL)
// hoặc USE <tenant_database> (MySQL)
public class SchemaTenantConnectionProvider implements MultiTenantConnectionProvider<String> {

    @Override
    public Connection getConnection(String tenantId) throws SQLException {
        Connection connection = dataSource.getConnection();
        connection.createStatement().execute("SET search_path = " + tenantId);
        return connection;
    }
}
```

### TODO: `service-kit-redis` — Distributed Lock Cho Multi-Tenant Operations

**Vấn đề:** Trong môi trường nhiều instance (horizontal scaling), các thao tác quan trọng theo Tenant (ví dụ: tạo schema mới, migrate dữ liệu) cần được khóa để tránh race condition giữa các Pod.

```java
// TODO: Implement trong service-kit-redis / TenantOperationLock
// Dùng Redisson để lock theo tenantId:
RLock lock = redissonClient.getLock("tenant-init:" + tenantId);
if (lock.tryLock(5, 30, TimeUnit.SECONDS)) {
    try {
        // Thao tác khởi tạo tenant an toàn
    } finally {
        lock.unlock();
    }
}
```
