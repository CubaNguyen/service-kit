# Service Kit Data

`service-kit-data` là một module thư viện nền tảng (core persistence library) dành cho các microservices sử dụng **Java 21**, **Spring Boot 3.4.x**, **Spring Data JPA** và **Hibernate 6.x**. Module này chuẩn hóa toàn bộ tầng truy cập dữ liệu (Data Access Layer), cung cấp sẵn mô hình Entity Lego linh hoạt, cơ chế xóa mềm (Soft Delete) trong suốt, tự động auditing thời gian, khóa lạc quan (Optimistic Locking), cấu hình tối ưu Connection Pool HikariCP và các tiện ích phân trang.

---

## 📑 Mục Lục (Table of Contents)

- [📌 1. Tổng Quan & Cài Đặt](#-1-tổng-quan--cài-đặt)
  - [Mục Đích Thư Viện](#mục-đích-thư-viện)
  - [Cài Đặt (Installation)](#cài-đặt-installation)
  - [Bảng Tra Cứu Nhanh (Quick Reference)](#bảng-tra-cứu-nhanh-quick-reference)
- [🏛️ 2. Mô Hình Phân Cấp Entity (Entity Hierarchy)](#️-2-mô-hình-phân-cấp-entity-entity-hierarchy)
- [📖 3. Hướng Dẫn Sử Dụng Cơ Bản (Quick Start)](#-3-hướng-dẫn-sử-dụng-cơ-bản-quick-start)
  - [3.1 Khai Báo Entity](#31-khai-báo-entity)
  - [3.2 Khai Báo Repository](#32-khai-báo-repository)
  - [3.3 Thực Hiện Xóa Mềm & Khôi Phục Dữ Liệu](#33-thực-hiện-xóa-mềm--khôi-phục-dữ-liệu)
- [🎯 4. 7 Query & Concurrency Patterns Thực Chiến](#-4-7-query--concurrency-patterns-thực-chiến)
  - [Pattern 1: Dynamic Filter/Search với Specification](#pattern-1-dynamic-filtersearch-với-specification-generic)
  - [Pattern 2: Tìm theo danh sách ID & findByIdOrThrow](#pattern-2-tìm-theo-danh-sách-id-findallbyid--tiện-ích-findbyidorthrow)
  - [Pattern 3: Chống N+1 Query với @EntityGraph](#pattern-3-chống-n1-query-với-entitygraph)
  - [Pattern 4: DTO Projection (selectMany)](#pattern-4-dto-projection-tối-ưu-hiệu-năng-truy-vấn-danh-sách)
  - [Pattern 5: Bulk Update theo điều kiện](#pattern-5-bulk-update-theo-điều-kiện)
  - [Pattern 6: Khóa lạc quan (Optimistic Locking)](#pattern-6-khóa-lạc-quan-optimistic-locking-với-version)
  - [Pattern 7: Khóa bi quan (Pessimistic Locking)](#pattern-7-khóa-bi-quan-pessimistic-locking-cho-nghiệp-vụ-thanh-toán)
- [⚠️ 5. Lưu Ý Thực Tế & Góc Khuất (Edge Cases & Gotchas)](#️-5-lưu-ý-thực-tế--góc-khuất-edge-cases--gotchas)
- [⚙️ 6. Tính Năng Nâng Cao & Tùy Chọn (Advanced Topics)](#️-6-tính-năng-nâng-cao--tùy-chọn-advanced-topics)
  - [6.1 Query Kernel Architecture (BaseRepositoryImpl)](#61-query-kernel-architecture-baserepositoryimpl)
  - [6.2 Dirty Checking: Update Quan Hệ Trong JPA](#62-dirty-checking-update-quan-hệ-trong-jpa)
  - [6.3 Chuyển Đổi Phân Trang (PageResponseMapper)](#63-chuyển-đổi-phân-trang-pageresponsemapper)
  - [6.4 Cấu Hình Connection Pool HikariCP](#64-cấu-hình-connection-pool-hikaricp-chuẩn-production)
  - [6.5 Viết Integration Test Nhanh (@DataJpaTest)](#65-viết-integration-test-nhanh-datajpatest)
  - [6.6 Integration Test Với Database Thật (Testcontainers)](#66-integration-test-với-database-thật-testcontainers)
  - [6.7 Database Migration (Flyway)](#67-tích-hợp-database-migration-với-flyway-optional)
  - [6.8 Audit Trail — Lịch Sử Thay Đổi (Hibernate Envers)](#68-audit-trail--lịch-sử-thay-đổi-hibernate-envers)
  - [6.9 Mã Hóa Cột DB (Field-Level Encryption)](#69-mã-hóa-cột-db-field-level-encryption)
  - [6.10 Observability & Monitoring](#610-observability--debug--monitoring)

---

## 📌 1. Tổng Quan & Cài Đặt

### Mục Đích Thư Viện

Thư viện giải quyết các bài toán nền tảng về cơ sở dữ liệu:
- **Chuẩn hóa Entity & Auditing**: Tự động sinh khóa chính `id`, tự động gán `createdAt` và `updatedAt` chuẩn UTC 13 chữ số (Epoch Milliseconds), chống ghi đè `null` vào `created_at` khi update.
- **Mô hình Entity Phân Cấp (Lego Hierarchy)**: Tách bạch rõ ràng giữa Entity thường, Entity có xóa mềm, Entity có khóa lạc quan (`@Version`) và Entity nghiệp vụ lõi (kết hợp cả hai) để không bị ép dùng thừa tính năng trên các bảng Logs / Atomic SQL Update.
- **Xóa mềm trong suốt (Transparent Soft Delete)**: Sử dụng Hibernate 6 `@SQLRestriction("is_deleted = false")` để tự động lọc dữ liệu đã xóa trên toàn bộ các câu truy vấn.
- **Base Repository toàn diện**: 
  - Kế thừa `JpaRepository` và `JpaSpecificationExecutor` cho tìm kiếm động.
  - Ghi đè toàn bộ hành vi `delete*()` để tự động chuyển thành xóa mềm.
  - Cung cấp `restoreById()` và `softDeleteAllByIds()` kèm cơ chế đồng bộ First-Level Cache (`flush` + `clear`).
  - Tiện ích `findByIdOrThrow()` giúp viết code Service ngắn gọn, tự ném `NotFoundException` chuẩn hóa.
  - Hỗ trợ tránh N+1 query qua `@EntityGraph` động (`findAllWithGraph`).
  - Hỗ trợ Spring Data projection tự động (`selectMany`) mà không cần viết câu lệnh Criteria hay DTO mapping thủ công.
- **Tối ưu hóa HikariCP Connection Pool**: Cung cấp bộ cấu hình mặc định chuẩn Production, tự động kích hoạt cảnh báo rò rỉ kết nối (`leakDetectionThreshold`) để phát hiện sớm các transaction bị treo.
- **Cầu nối Phân trang**: Tiện ích `PageResponseMapper` chuyển đổi 1 dòng code từ `Page<T>` của JPA sang `PageResponse<T>` của `service-kit-common`.

### Cài Đặt (Installation)

Thêm dependency sau vào file `pom.xml` của service:

```xml
<dependency>
    <groupId>com.servicekit</groupId>
    <artifactId>service-kit-data</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> [!NOTE]
> Module `service-kit-data` tự động kéo theo **`service-kit-common`** dưới dạng Transitive Dependency. Bạn có thể sử dụng trực tiếp các class dùng chung như `PageResponse<T>`, `NotFoundException`, và `TimeUtils` mà không cần khai báo thêm dependency `service-kit-common` thủ công trong `pom.xml`.
>
> Ngoài ra, module đã tích hợp sẵn Spring Boot Auto-Configuration (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), tự động cấu hình DataSource và HikariCP khi ứng dụng khởi động.

### Bảng Tra Cứu Nhanh (Quick Reference)

| Thành phần | Class / Annotation | Mục đích |
|---|---|---|
| **Base Entity** | `BaseEntity` | MappedSuperclass siêu nhẹ chứa `id` (UUID v7), `createdAt`, `updatedAt` và Lifecycle callback. Tự động set thời gian qua `TimeUtils.nowEpochMilli()`. |
| **Soft Delete Entity** | `SoftDeletableEntity` | Kế thừa `BaseEntity`, thêm `is_deleted` và `@SQLRestriction("is_deleted = false")`. |
| **Auditable Entity** | `AuditableEntity` | Kế thừa `SoftDeletableEntity`, thêm `createdBy`/`updatedBy` qua Spring Data JPA Auditing. |
| **Versioned Entity** | `VersionedEntity` | Kế thừa `BaseEntity`, thêm `@Version` (dành cho bảng cần khóa lạc quan nhưng xóa cứng). |
| **Core Domain Entity** | `VersionedSoftDeletableEntity` | Kế thừa `SoftDeletableEntity` + `@Version` + `createdBy`/`updatedBy` (chuẩn cho User, Order, Product...). |
| **Base Repository** | `BaseRepository<T, ID>` | Mở rộng `JpaRepository` + `JpaSpecificationExecutor`, thêm `restoreById()`, `softDeleteAllByIds()`, `findByIdOrThrow()`, `findAllWithGraph()`, `selectMany()`. |
| **Base Repo Impl** | `BaseRepositoryImpl<T, ID>` | Implement xóa mềm toàn diện cho các hàm delete, xử lý native SQL cho restore, và cung cấp giải pháp dynamic EntityGraph & dynamic projection. |
| **Cấu hình Hikari** | `HikariDefaultsProperties` | Configuration properties (`service-kit.datasource.hikari.*`) chứa các thông số pool chuẩn. |
| **Auto Configuration** | `DataSourceAutoConfiguration` | Tự động apply cấu hình kiểm soát rò rỉ connection vào `HikariDataSource`. |
| **Phân trang Adapter** | `PageResponseMapper` | Tiện ích map `org.springframework.data.domain.Page<T>` sang `common.PageResponse<T>`. |

---

## 🏛️ 2. Mô Hình Phân Cấp Entity (Entity Hierarchy)

Tùy vào tính chất của bảng dữ liệu trong thực tế, hãy chọn đúng lớp Entity để kế thừa:

```
                        BaseEntity (uuid id, createdAt, updatedAt)
                       /          \
     SoftDeletableEntity          VersionedEntity
     (thêm is_deleted)            (thêm @Version)
           |         \
     AuditableEntity  VersionedSoftDeletableEntity
  (+ createdBy/updatedBy)  (is_deleted + @Version + createdBy/updatedBy)
```

### 📋 Hướng Dẫn Chọn Lớp Kế Thừa:

| Loại Entity | Thuộc tính có sẵn | Khi nào nên dùng trong thực tế? |
|---|---|---|
| **`BaseEntity`** | `uuid id`, `createdAt`, `updatedAt` | • **Bảng chỉ ghi (Append-only / Logs):** `system_logs`, `audit_trail`, `user_activities`.<br>• **Bảng dùng Atomic SQL Update:** `wallet`, `bank_accounts` (`UPDATE ... WHERE balance >= 100`).<br>• **Bảng Last-Write-Wins:** `driver_gps_locations`, `device_telemetry`. |
| **`SoftDeletableEntity`** | `BaseEntity` + `isDeleted` | • Danh mục tĩnh, cấu hình hệ thống cần xóa mềm nhưng không cần audit người sửa. |
| **`AuditableEntity`** | `SoftDeletableEntity` + `createdBy` + `updatedBy` | • Entity cần biết **ai** tạo/sửa nhưng không cần Optimistic Locking (content, settings...). |
| **`VersionedEntity`** | `BaseEntity` + `@Version` | • Bảng cần chống race condition nhưng xóa cứng. |
| **`VersionedSoftDeletableEntity`** | `SoftDeletableEntity` + `@Version` + `createdBy` + `updatedBy` | • **🌟 Nghiệp vụ cốt lõi (Core Business Domain):** `User`, `Product`, `Order`, `Contract` — đầy đủ tính năng. |

---

## 📖 3. Hướng Dẫn Sử Dụng Cơ Bản (Quick Start)

### 3.1 Khai Báo Entity

#### Ví dụ 1: Bảng Log / Giao dịch (Kế thừa `BaseEntity` - Siêu nhẹ)
```java
@Getter
@Setter
@Entity
@Table(name = "payment_logs")
public class PaymentLogEntity extends BaseEntity {
    private String transactionId;
    private Long amount;
    private String status;
}
```

#### Ví dụ 2: Bảng Nghiệp vụ Core (Kế thừa `VersionedSoftDeletableEntity` - Đầy đủ tính năng)
```java
@Getter
@Setter
@Entity
@Table(name = "products")
public class ProductEntity extends VersionedSoftDeletableEntity {
    private String name;
    private Double price;
    private Integer stock;
}
```

---

### 3.2 Khai Báo Repository

Tất cả Repository của bạn nên kế thừa `BaseRepository<T, ID>`:

```java
package com.servicekit.example.repository;

import com.servicekit.data.repository.BaseRepository;
import com.servicekit.example.entity.ProductEntity;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ProductRepository extends BaseRepository<ProductEntity, UUID> {
    boolean existsByName(String name);
}
```

#### Bật `repositoryBaseClass` trong ứng dụng chính:
> [!IMPORTANT]
> **Bắt buộc cấu hình `repositoryBaseClass`**: Để Spring Data JPA kích hoạt toàn bộ cơ chế Xóa mềm tự động (`BaseRepositoryImpl`) cho các Repository của bạn, hãy khai báo `repositoryBaseClass = BaseRepositoryImpl.class` tại class Application chính:

```java
@SpringBootApplication
@EnableJpaRepositories(
    basePackages = "com.servicekit",
    repositoryBaseClass = BaseRepositoryImpl.class
)
public class Application {
    public static void main(String[] args) {
        SpringApplication.run(Application.class, args);
    }
}
```

---

### 3.3 Thực Hiện Xóa Mềm & Khôi Phục Dữ Liệu

```java
@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    // 1. Xóa mềm 1 entity
    public void deleteProduct(UUID id) {
        productRepository.deleteById(id); // is_deleted = true, updated_at = now
    }

    // 2. Xóa mềm danh sách entity
    public void deleteMultiple(List<UUID> ids) {
        productRepository.deleteAllById(ids); 
        // Hoặc bulk update hiệu năng cao: productRepository.softDeleteAllByIds(ids);
    }

    // 3. Khôi phục lại bản ghi đã bị xóa mềm
    public void restoreProduct(UUID id) {
        productRepository.restoreById(id); // is_deleted = false
    }

    // 4. Tìm kiếm tự động ném NotFoundException (404)
    public ProductEntity getProduct(UUID id) {
        return productRepository.findByIdOrThrow(id);
    }
}
```

---

## 🎯 4. 7 Query & Concurrency Patterns Thực Chiến

### Pattern 1: Dynamic Filter/Search với Specification (Generic)
`BaseRepository` đã tích hợp sẵn `JpaSpecificationExecutor<T>`, cho phép tìm kiếm linh hoạt nhiều điều kiện tùy chọn:

```java
public class ProductSpecs {
    public static Specification<ProductEntity> hasName(String name) {
        return (root, query, cb) -> (name == null || name.isBlank()) 
                ? null 
                : cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    public static Specification<ProductEntity> minPrice(Double minPrice) {
        return (root, query, cb) -> minPrice == null 
                ? null 
                : cb.greaterThanOrEqualTo(root.get("price"), minPrice);
    }
}

// Tại Service:
Specification<ProductEntity> spec = Specification.where(ProductSpecs.hasName(name))
                                                 .and(ProductSpecs.minPrice(minPrice));
Page<ProductEntity> result = productRepository.findAll(spec, pageable);
```

### Pattern 2: Tìm theo danh sách ID (`findAllById`) & Tiện ích `findByIdOrThrow`
- Dùng `findAllById(ids)` có sẵn từ JPA để batch load danh sách bản ghi theo ID thay vì loop query trong code.
- Dùng `findByIdOrThrow(id)` được cung cấp sẵn bởi `BaseRepository` để code tầng Service không phải lặp lại đoạn `.orElseThrow(() -> new NotFoundException(...))`.

### Pattern 3: Chống N+1 Query với `@EntityGraph`
Tránh lỗi `LazyInitializationException` hoặc bắn hàng chục câu query phụ khi load quan hệ `@OneToMany` / `@ManyToOne`. Có 2 cách thực hiện:

#### Cách 1: Dùng `@EntityGraph` tĩnh trên Repository (Khi biết trước quan hệ cần load)
```java
public interface UserRepository extends BaseRepository<UserEntity, UUID> {

    @EntityGraph(attributePaths = {"orders", "profile"})
    Optional<UserEntity> findWithDetailsById(UUID id);
}
```

#### Cách 2: Dùng `findAllWithGraph` động từ `BaseRepository` (Khi cần load quan hệ linh hoạt kèm `Specification`)
```java
Specification<ProductEntity> spec = ProductSpecs.hasName("MacBook");

// Không cần Sort — mặc định Sort.unsorted()
List<ProductEntity> products = productRepository.findAllWithGraph(spec, "categories", "supplier");

// Hoặc truyền Sort tùy chọn
List<ProductEntity> sorted = productRepository.findAllWithGraph(
        spec,
        Sort.by(Sort.Direction.DESC, "createdAt"),
        "categories", "supplier"
);
```

> [!NOTE]
> `findAllWithGraph` tự động áp `distinct(true)` để tránh duplicate root entity khi SQL JOIN với collection.
> **Nguyên nhân duplicate là do SQL JOIN**, không phải EntityGraph tự sinh ra.
> Nếu chỉ fetch quan hệ `@ManyToOne` (không phải collection), bạn không cần lo về duplicate.

---

### Pattern 4: DTO Projection (Tối ưu hiệu năng truy vấn danh sách)
Khi API danh sách chỉ cần vài trường (`id`, `name`) thay vì tải cả Entity có hàng chục cột kèm quan hệ. Có 2 cách thực hiện:

#### Cách 1: Constructor DTO Projection qua JPQL `@Query`
```java
public record ProductSummaryDto(UUID id, String name, Double price) {}

public interface ProductRepository extends BaseRepository<ProductEntity, UUID> {

    @Query("SELECT new com.servicekit.example.dto.ProductSummaryDto(p.id, p.name, p.price) FROM ProductEntity p")
    Page<ProductSummaryDto> findAllSummary(Pageable pageable);
}
```

#### Cách 2: Dùng `selectMany` động của `BaseRepository` (Không cần viết câu lệnh SQL/JPQL)
Spring Data JPA hỗ trợ cả **Interface-based Projections** và **Class-based Projections (DTOs)**.

**Bước 1: Khai báo Interface Projection hoặc Record DTO**
```java
// Dạng Interface
public interface ProductInfoProjection {
    UUID getId();
    String getName();
    Double getPrice();
}

// Hoặc dạng Record DTO (Spring Data tự động map theo tên constructor/properties)
public record ProductSummaryDto(UUID id, String name, Double price) {}
```

**Bước 2: Gọi `selectMany` truyền vào Class target**
```java
Specification<ProductEntity> spec = ProductSpecs.minPrice(1000.0);

// 1. Lấy danh sách dạng Interface Projection
List<ProductInfoProjection> summaries = productRepository.selectMany(spec, ProductInfoProjection.class);

// 2. Lấy danh sách dạng Record DTO
List<ProductSummaryDto> dtos = productRepository.selectMany(spec, ProductSummaryDto.class);
```

### Pattern 5: Bulk Update theo điều kiện
Khi cần cập nhật trạng thái hàng loạt theo điều kiện mà không cần load entity lên RAM:

```java
public interface ProductRepository extends BaseRepository<ProductEntity, UUID> {

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("UPDATE #{#entityName} p SET p.price = p.price * 1.1, p.updatedAt = :now WHERE p.id IN :ids")
    int increasePriceByIds(@Param("ids") List<UUID> ids, @Param("now") Long now);
}
```

### Pattern 6: Khóa lạc quan (Optimistic Locking) với `@Version`
Khi sử dụng `VersionedEntity` hoặc `VersionedSoftDeletableEntity`, nếu có 2 request cùng sửa đồng thời một bản ghi, JPA tự động kiểm tra số version khi sinh câu SQL `UPDATE ... WHERE version = ?`. Nếu bị ghi đè, hệ thống sẽ ném `OptimisticLockException` giúp chống mất dữ liệu (Lost Update).

### Pattern 7: Khóa bi quan (Pessimistic Locking) cho nghiệp vụ thanh toán
Dùng cho các nghiệp vụ thanh toán cần khóa dòng trực tiếp dưới DB (`SELECT ... FOR UPDATE`):

```java
public interface WalletRepository extends BaseRepository<WalletEntity, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT w FROM WalletEntity w WHERE w.id = :id")
    Optional<WalletEntity> findByIdForUpdate(@Param("id") UUID id);
}
```

---

## ⚠️ 5. Lưu Ý Thực Tế & Góc Khuất (Edge Cases & Gotchas)

Dưới đây là các góc khuất thực tế quan trọng khi áp dụng JPA, Soft Delete và Concurrent Updates:

| # | Vấn đề / Bẫy thực tế | Tác động | Giải pháp chuẩn |
|---|---|---|---|
| **1** | **Soft Delete Cascade Không Tự Động (Thiết Kế Cố Ý)** | `@OneToMany` + Soft Delete KHÔNG tự cascade ngầm. Nếu gọi `orderRepository.deleteById(orderId)`, `Order` bị `is_deleted = true` nhưng các `OrderItem` con **không bị xóa theo**. | **Thiết kế chuẩn Microservices (Explicit Cascade):** Xử lý cascade thủ công tại Service bằng Bulk Update:<br>`orderRepository.deleteById(orderId);`<br>`orderItemRepository.softDeleteAllByOrderId(orderId);`<br>*Lý do:* Tránh bẫy N+1 UPDATE Query ngầm của Hibernate và đảm bảo kiểm soát luồng dữ liệu minh bạch, tránh vô tình xóa lan dây chuyền. |
| **2** | **`@SQLRestriction` Bị Bypass Bởi Native Query** | `@SQLRestriction("is_deleted = false")` KHÔNG có hiệu lực với Native Query, `JdbcTemplate` hoặc Flyway. Native Query sẽ trả về cả record đã xóa mềm. | Dev viết Native Query phải **tự thêm điều kiện** `WHERE is_deleted = false` thủ công vào câu lệnh SQL. |
| **3** | **Unique Constraint + Soft Delete** | Cột có Unique Index (ví dụ `email`), khi xóa mềm bản ghi cũ, người dùng mới đăng ký lại cùng email đó sẽ bị ném lỗi `Duplicate entry`. | **PostgreSQL:** Tạo Partial Unique Index:<br>`CREATE UNIQUE INDEX idx_users_email_active ON users(email) WHERE is_deleted = false;`<br>**Khác:** Tạo Composite Index `(email, deleted_at)` (với `deleted_at` mặc định = 0). |
| **4** | **Lazy Loading Quan Hệ Soft Deleted** | Entity A `@ManyToOne` B (B đã bị xóa mềm). Khi gọi `a.getB().getName()`, Hibernate sẽ ném `EntityNotFoundException`. | Gắn annotation `@NotFound(action = NotFoundAction.IGNORE)` của Hibernate lên trường `@ManyToOne` để trả về `null` thay vì throw exception. |
| **5** | **`orphanRemoval = true` Rò Rỉ Soft Delete** | Gọi `collection.remove()` khi dùng `@OneToMany(orphanRemoval = true)` sẽ bắn câu lệnh `DELETE` SQL vật lý, bỏ qua logic xóa mềm. | **KHÔNG DÙNG** `orphanRemoval = true` cho thực thể có xóa mềm. Thao tác xóa con thủ công qua Repository (`deleteById`). |
| **6** | **Bulk Update Bypass `@Version` & Envers** | `softDeleteAllByIds()` dùng JPQL Bulk Update sẽ **KHÔNG** tăng version `@Version` và **KHÔNG** ghi nhận lịch sử vào Hibernate Envers. | Dùng `deleteAllById()` (lặp save từng entity) nếu cần Audit đầy đủ. Chỉ dùng `softDeleteAllByIds()` khi ưu tiên tốc độ số lượng lớn. |
| **7** | **`GenerationType.IDENTITY` Disable Batch Insert** | Dùng ID tự tăng `IDENTITY` làm JDBC driver buộc phải insert từng dòng để lấy ID, làm mất tác dụng của JDBC Batch Insert. | Đã được giải quyết sẵn: `service-kit-data` mặc định sử dụng **UUID v7** (sequential timestamp-based UUID), tương thích hoàn hảo với JDBC Batching. |
| **8** | **Mocking Time Trong Unit Test** | Hàm Lifecycle callback `onCreate`/`onUpdate` lấy thời gian thực từ hệ thống (`TimeUtils`), gây khó khăn khi viết test phụ thuộc thời gian. | Sử dụng `TimeUtils.setClock(clock)` trong `@BeforeEach` của Unit Test để cố định thời gian giả lập. |

---

## ⚙️ 6. Tính Năng Nâng Cao & Tùy Chọn (Advanced Topics)

### 6.1 Query Kernel Architecture (`BaseRepositoryImpl`)

`BaseRepositoryImpl` được thiết kế theo nguyên tắc **một điểm dựng query duy nhất** — tương tự vai trò của `IQueryable<T>` trong Entity Framework:

```
           buildQuery(spec, sort, entityGraph?)   ← Kernel duy nhất
                    │
        ┌───────────┼───────────┐
        ↓           ↓           ↓
 findAllWithGraph  findAll    [Pagination - TODO]
   (+ graph hint)  (via JPA)
```

| Protected Method | Vai trò |
|---|---|
| `buildQuery(spec, sort, graph)` | Kernel: dựng `TypedQuery<T>`, áp Spec + Sort + EntityGraph hint. Mọi read method đều gọi xuống đây. |
| `buildQuery(spec, sort)` | Overload không cần EntityGraph — cho các query không eager load. |
| `withDistinct(spec)` | Helper: bọc Spec để inject `distinct(true)` vào CriteriaQuery. Dùng khi SQL sẽ JOIN collection để tránh duplicate root entity. **Lưu ý**: duplicate là do SQL JOIN, không phải do EntityGraph tự sinh ra. |
| `buildEntityGraph(paths...)` | Helper: dựng `EntityGraph<T>` động từ danh sách attribute path. Trả về `null` nếu không có path → kernel tự bỏ qua hint. |

> [!TIP]
> Khi implement **Pagination** (TODO) và **AdvancedFilter** (TODO) về sau, cả hai đều sẽ gọi `buildQuery()` và `getCountQuery()` làm nền tảng — không tự dựng query riêng.

---

### 6.2 Dirty Checking: Update Quan Hệ Trong JPA

> ⚠️ **Lưu ý quan trọng cho dev chuyển từ MyBatis / Raw SQL sang JPA:**
> Trong JPA **không tồn tại pattern "UPDATE JOIN"** như câu lệnh SQL thuần.
> Thay vào đó, JPA hoạt động theo cơ chế **Managed Entity & Dirty Checking**:
> 1. Load Entity gốc ra trong `@Transactional`.
> 2. Thao tác trực tiếp lên thuộc tính hoặc danh sách con (collection) trong RAM.
> 3. Khi Transaction kết thúc (commit), JPA tự động phát hiện thay đổi và sinh câu lệnh SQL `UPDATE`/`INSERT` tương ứng.

```java
@Transactional
public void addOrderToUser(UUID userId, OrderEntity newOrder) {
    // 1. Load user lên RAM (Managed state)
    UserEntity user = userRepository.findByIdOrThrow(userId);
    
    // 2. Thêm trực tiếp vào collection
    user.getOrders().add(newOrder);
    newOrder.setUser(user);
    
    // 3. Không cần gọi userRepository.save(user)! JPA tự động INSERT order và UPDATE FK khi commit.
}
```

---

### 6.3 Chuyển Đổi Phân Trang (`PageResponseMapper`)

Kết hợp phân trang của Spring Data JPA với `PageResponse` của module `service-kit-common`:

```java
@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductRepository productRepository;

    @GetMapping
    public PageResponse<ProductDto> getProducts(Pageable pageable) {
        Page<ProductEntity> page = productRepository.findAll(pageable);
        
        // Map 1 dòng từ Page<ProductEntity> sang PageResponse<ProductDto> của service-kit-common
        return PageResponseMapper.from(page, p -> ProductDto.builder()
                .id(p.getId())
                .name(p.getName())
                .price(p.getPrice())
                .createdAt(p.getCreatedAt())
                .build());
    }
}
```

---

### 6.4 Cấu Hình Connection Pool HikariCP Chuẩn Production

Module cung cấp sẵn các giá trị mặc định tối ưu qua prefix `service-kit.datasource.hikari`.

Bạn có thể tùy biến linh hoạt trong file `application.yml`:

```yaml
service-kit:
  datasource:
    hikari:
      maximum-pool-size: 15       # Số connection tối đa (mặc định: 10)
      minimum-idle: 5             # Số connection nhàn rỗi tối thiểu (mặc định: 5)
      connection-timeout: 20000   # Timeout lấy connection (ms) (mặc định: 20s)
      idle-timeout: 300000        # Timeout đóng connection nhàn rỗi (ms) (mặc định: 5 phút)
      max-lifetime: 1800000       # Tuổi thọ tối đa của 1 connection (ms) (mặc định: 30 phút)
      leak-detection-threshold: 60000 # Cảnh báo rò rỉ connection nếu mượn > 60s (mặc định: 60s)
      pool-name: "OrderServiceHikariPool" # Tên pool hiển thị trên Metrics/Logs
```

---

### 6.5 Viết Integration Test Nhanh (`@DataJpaTest`)

Ví dụ test tính năng Xóa mềm và Restore bằng `@DataJpaTest`:

```java
package com.servicekit.example;

import com.servicekit.data.repository.BaseRepositoryImpl;
import com.servicekit.example.entity.ProductEntity;
import com.servicekit.example.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@EnableJpaRepositories(basePackages = "com.servicekit", repositoryBaseClass = BaseRepositoryImpl.class)
class ProductRepositoryTest {

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testSoftDeleteAndRestore() {
        // 1. Tạo và lưu entity
        ProductEntity product = new ProductEntity();
        product.setName("MacBook Pro");
        product.setPrice(2000.0);
        product = productRepository.save(product);
        UUID productId = product.getId();

        assertThat(product.getCreatedAt()).isNotNull();

        // 2. Xóa mềm
        productRepository.deleteById(productId);

        // Kiểm tra @SQLRestriction: findAll và findById không tìm thấy
        Optional<ProductEntity> deletedProduct = productRepository.findById(productId);
        assertThat(deletedProduct).isEmpty();

        // 3. Khôi phục (Restore)
        productRepository.restoreById(productId);

        // Kiểm tra lại: entity đã xuất hiện trở lại
        Optional<ProductEntity> restoredProduct = productRepository.findById(productId);
        assertThat(restoredProduct).isPresent();
        assertThat(restoredProduct.get().getIsDeleted()).isFalse();
    }
}
```

---

### 6.6 Integration Test Với Database Thật (`Testcontainers`)

Để verify chính xác 100% các tính năng nhạy cảm với SQL như `@SQLRestriction`, Partial Unique Index hoặc Native Restore trên Database thật (PostgreSQL / MySQL):

```java
@Testcontainers
@SpringBootTest
class ProductRepositoryTestcontainersTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    private ProductRepository productRepository;

    @Test
    void testSoftDeleteWithRealPostgres() {
        // Test trên PostgreSQL container thật
    }
}
```

---

### 6.7 Tích Hợp Database Migration Với Flyway (Optional)

`service-kit-data` đã khai báo sẵn dependency `flyway-core` với thẻ `<optional>true</optional>`. 

Nếu service của bạn muốn áp dụng cơ chế Version Control cho database schema, chỉ cần bật Flyway trong `pom.xml` của service và tạo các file SQL migration theo quy chuẩn tại `src/main/resources/db/migration`:

```
src/main/resources/db/migration/
├── V1__create_products_table.sql
├── V2__add_index_products.sql
```

Ví dụ cấu hình trong `application.yml`:
```yaml
spring:
  flyway:
    enabled: true
    baseline-on-migrate: true
    locations: classpath:db/migration
```

---

### 6.8 Audit Trail — Lịch Sử Thay Đổi (Hibernate Envers)

> [!NOTE]
> Tính năng này **optional** — chỉ kích hoạt khi service thêm dependency `hibernate-envers`. Module cung cấp sẵn `ServiceKitRevisionEntity` và `ServiceKitRevisionListener`.

Hibernate Envers tự động lưu toàn bộ lịch sử thay đổi (INSERT/UPDATE/DELETE) của Entity vào bảng `*_aud`, kèm thông tin `revision_id`, `timestamp` và `modified_by`.

#### Cách bật:

**Bước 1:** Thêm dependency vào `pom.xml` của service:
```xml
<dependency>
    <groupId>org.hibernate.orm</groupId>
    <artifactId>hibernate-envers</artifactId>
</dependency>
```

**Bước 2:** Bật JPA Auditing và Envers trong Application:
```java
@SpringBootApplication
@EnableJpaAuditing
@EnableEnvers
public class Application { ... }
```

**Bước 3:** Gắn `@Audited` vào Entity muốn theo dõi lịch sử:
```java
import org.hibernate.envers.Audited;

@Audited
@Entity
@Table(name = "products")
public class ProductEntity extends VersionedSoftDeletableEntity {
    private String name;
    private Double price;
}
```

#### Truy vấn Lịch Sử:

```java
@Service
@RequiredArgsConstructor
public class ProductAuditService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<Object[]> getHistory(UUID productId) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.createQuery()
                .forRevisionsOfEntity(ProductEntity.class, false, true)
                .add(AuditEntity.id().eq(productId))
                .addOrder(AuditEntity.revisionNumber().asc())
                .getResultList();
    }

    public ProductEntity getAtRevision(UUID productId, int revisionNumber) {
        AuditReader reader = AuditReaderFactory.get(entityManager);
        return reader.find(ProductEntity.class, productId, revisionNumber);
    }
}
```

---

### 6.9 Mã Hóa Cột DB (Field-Level Encryption)

> [!IMPORTANT]
> Dùng cho dữ liệu PII (CCCD, số thẻ, địa chỉ...) để tuân thủ GDPR/PCI-DSS.
> Sử dụng thuật toán **AES-256-GCM** — mã hóa đối xứng có xác thực tính toàn vẹn.

Module cung cấp sẵn [`EncryptedStringConverter`](./src/main/java/com/servicekit/data/converter/EncryptedStringConverter.java).

#### Cấu hình khóa bí mật:

```bash
# Tạo khóa 256-bit ngẫu nhiên:
openssl rand -base64 32
# Kết quả ví dụ: K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols=
```

Thiết lập khóa qua biến môi trường (KHÔNG hardcode vào code):
```bash
export SERVICE_KIT_ENCRYPTION_KEY="K7gNU3sdo+OL0wNhqoVWhr3g6s1xYv72ol/pe/Unols="
```

#### Cách dùng trên Entity:

```java
import com.servicekit.data.converter.EncryptedStringConverter;

@Entity
@Table(name = "customers")
public class CustomerEntity extends VersionedSoftDeletableEntity {

    private String fullName; // Không mã hóa — tìm kiếm bình thường được

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "national_id")
    private String nationalId; // Lưu DB dạng Base64(IV+CipherText+Tag), đọc ra là plaintext tự động

    @Convert(converter = EncryptedStringConverter.class)
    @Column(name = "phone_number")
    private String phoneNumber;
}
```

> [!WARNING]
> **Hạn chế quan trọng:** Cột được mã hóa **không thể tìm kiếm trực tiếp** (`WHERE national_id = ?`) vì giá trị trong DB là ciphertext ngẫu nhiên mỗi lần mã hóa.
> Nếu cần tìm kiếm, hãy lưu thêm hash (SHA-256) của giá trị vào cột riêng để index và so sánh.

---

### 6.10 Observability & Monitoring

#### P6Spy — Debug SQL & Phát Hiện N+1 Query (Chỉ Dev)

Thêm vào `pom.xml` của service cần debug (**KHÔNG dùng production**):
```xml
<dependency>
    <groupId>com.github.gavlyukovskiy</groupId>
    <artifactId>p6spy-spring-boot-starter</artifactId>
    <version>1.9.2</version>
    <scope>runtime</scope>
</dependency>
```

P6Spy tự động log toàn bộ SQL kèm thời gian thực thi — giúp detect N+1 và slow query ngay ở local.

#### Micrometer — HikariCP Pool Metrics (Production)

Bật trong `application.yml` (cần `spring-boot-starter-actuator`):
```yaml
management:
  metrics:
    enable:
      hikaricp: true
  endpoints:
    web:
      exposure:
        include: "health,metrics,prometheus"
```

Các metric quan trọng để alert:
- `hikaricp.connections.active` — số connection đang dùng
- `hikaricp.connections.pending` — số request đang chờ connection (> 0 lâu = dấu hiệu pool exhaustion)
- `hikaricp.connections.timeout.total` — tổng số lần timeout lấy connection
