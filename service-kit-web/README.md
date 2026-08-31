# Service Kit — Web Layer 🌐

Module `service-kit-web` cung cấp Auto-Configuration và các thành phần chuyên biệt cho tầng **HTTP / Web API** trong kiến trúc Microservices. Bằng cách tách riêng phần Web khỏi `service-kit-common`, các service phi-Web (Kafka Consumers, Background Workers) không bị ép tải lên toàn bộ Tomcat và Spring Web MVC.

---

## 📑 Mục Lục (Table of Contents)
- [📌 1. Tổng Quan & Cài Đặt](#-1-tổng-quan--cài-đặt)
  - [Phạm Vi Module (Scope)](#phạm-vi-module-scope)
  - [Cài Đặt (Installation)](#cài-đặt-installation)
  - [Điều Kiện Tiên Quyết (Prerequisite)](#điều-kiện-tiên-quyết-prerequisite)
  - [Bộ Lọc Điều Kiện (Guard Condition)](#bộ-lọc-điều-kiện-guard-condition)
  - [Bảng Tra Cứu Nhanh (Quick Reference)](#bảng-tra-cứu-nhanh-quick-reference)
- [🎯 2. WebExceptionHandler — Bộ Map Lỗi HTTP Đầy Đủ](#-2-webexceptionhandler--bộ-map-lỗi-http-đầy-đủ)
  - [Quy Ước Thứ Tự Ưu Tiên (@Order Hierarchy)](#quy-ước-thứ-tự-ưu-tiên-order-hierarchy)
  - [Bảng Map Exception → HTTP Status](#bảng-map-exception--http-status)
  - [Mẫu Response Body (JSON Envelope)](#mẫu-response-body-json-envelope)
- [🛡️ 3. Pagination Guard — Chống DB Overload](#️-3-pagination-guard--chống-db-overload)
  - [Cơ Chế Hoạt Động (Customizer Pattern)](#cơ-chế-hoạt-động-customizer-pattern)
  - [Cấu Hình & Hiệu Ứng Thực Tế](#cấu-hình--hiệu-ứng-thực-tế)
- [💻 4. Ví Dụ Thực Chiến End-to-End](#-4-ví-dụ-thực-chiến-end-to-end)
  - [4.1 Kịch Bản Optimistic Lock Conflict (Race Condition)](#41-kịch-bản-optimistic-lock-conflict-race-condition)
  - [4.2 Kịch Bản Client Gửi Tham Số Không Hợp Lệ](#42-kịch-bản-client-gửi-tham-số-không-hợp-lệ)
- [⚠️ 5. Lưu Ý Thực Tế & Góc Khuất (Edge Cases & Gotchas)](#️-5-lưu-ý-thực-tế--góc-khuất-edge-cases--gotchas)
- [🔲 6. Roadmap](#-6-roadmap)

---

## 📌 1. Tổng Quan & Cài Đặt

### Phạm Vi Module (Scope)

> [!IMPORTANT]
> **Inbound HTTP Server Only.** Module này chỉ xử lý request đi **VÀO** service (inbound). Các lỗi liên quan đến gọi HTTP ra service khác (RestTemplate, Feign, WebClient) thuộc phạm vi module `service-kit-client` (planned) — không nhét vào đây để tránh lẫn lộn hai mối quan tâm khác nhau.

### Cài Đặt (Installation)
Thêm vào `pom.xml` của API Service có giao tiếp HTTP:

```xml
<dependency>
    <groupId>com.servicekit</groupId>
    <artifactId>service-kit-web</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

> [!NOTE]
> Module `service-kit-web` tự động kéo theo **`service-kit-common`** dưới dạng Transitive Dependency. Bạn có thể dùng trực tiếp `ApiResponse<T>`, `ErrorCode`, và `TimeUtils` mà không cần khai báo lại `service-kit-common` trong `pom.xml`.

### Điều Kiện Tiên Quyết (Prerequisite)

Để tính năng **bắt lỗi Optimistic Lock (HTTP 409)** hoạt động, các Entity dưới Database **bắt buộc** phải dùng cơ chế khóa lạc quan thông qua các class kế thừa từ `service-kit-data`:

| Entity Base Class | Mô tả |
|---|---|
| `VersionedEntity` | Khóa lạc quan + xóa vật lý |
| `VersionedSoftDeletableEntity` | **🌟 Khuyến nghị** — Khóa lạc quan + xóa mềm + full auditing |

> [!WARNING]
> Nếu Entity chỉ kế thừa `BaseEntity` hoặc `SoftDeletableEntity` thông thường (không có cột `@Version`), Hibernate cập nhật dữ liệu theo nguyên lý *Last-Write-Wins* (ghi đè vô điều kiện). Hệ thống **sẽ không bao giờ phát sinh** `OptimisticLockException` và handler 409 sẽ không có tác dụng.

### Bộ Lọc Điều Kiện (Guard Condition)
Toàn bộ các class trong module được bảo vệ bởi:
```java
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
```
Nhất quán với pattern của `service-kit-common`. Spring Boot tự động bỏ qua toàn bộ cấu hình này nếu ứng dụng không chạy ở chế độ Servlet Web (Worker/Batch/Console).

### Bảng Tra Cứu Nhanh (Quick Reference)

| Thành phần | Class | Mục đích |
|---|---|---|
| **Exception Handler** | `WebExceptionHandler` | Map các lỗi HTTP hạ tầng về `ApiResponse` envelope chuẩn với đúng HTTP status |
| **Pagination Guard** | `PaginationGuardConfiguration` | Giới hạn `pageSize` tối đa qua `PageableHandlerMethodArgumentResolverCustomizer` |
| **Properties** | `WebProperties` | Cấu hình `service-kit.web.*` cho toàn module |

---

## 🎯 2. WebExceptionHandler — Bộ Map Lỗi HTTP Đầy Đủ

### Quy Ước Thứ Tự Ưu Tiên (@Order Hierarchy)

Nhằm đảm bảo tính tất định (deterministic), không bị phụ thuộc vào thứ tự quét bean ngẫu nhiên của Spring, hệ thống quy định phân cấp rõ ràng giữa các `@RestControllerAdvice` trong toàn bộ hệ sinh thái `service-kit`:

| Module | Advice Class | `@Order` | Trách nhiệm chính |
|---|---|---|---|
| `service-kit-security` | `SecurityExceptionHandler` | **`@Order(10)`** | Bắt lỗi xác thực & phân quyền: `AuthenticationException` (401), `AccessDeniedException` (403). Cần được xét đầu tiên. |
| `service-kit-web` | `WebExceptionHandler` | **`@Order(20)`** | Bắt lỗi hạ tầng HTTP: 400 (TypeMismatch, Param Validation, Malformed JSON), 404 (NoResourceFound), 405 (MethodNotAllowed), 409 (OptimisticLock), 413 (MaxUploadSize). |
| `service-kit-common` | `GlobalExceptionHandler` | **`@Order(LOWEST_PRECEDENCE)`** | Fallback kernel: bắt `MethodArgumentNotValidException` (body `@Valid`), `BaseBusinessException` và catch-all `Exception.class` (500). |

### Bảng Map Exception → HTTP Status

| Exception | HTTP Status | Nguồn gốc | Ghi chú |
|---|---|---|---|
| `ObjectOptimisticLockingFailureException` | **409 Conflict** | Spring Data JPA `repository.save()` | Phổ biến nhất khi dùng `@Version` |
| `OptimisticLockException` | **409 Conflict** | JPA `EntityManager` trực tiếp | Dùng khi gọi JPQL update tay có `@Version` |
| `NoResourceFoundException` | **404 Not Found** | Spring Boot 3.4.x / Spring 6.2+ | Tự động bắt khi gọi sai route, không cần config thêm |
| `HttpRequestMethodNotSupportedException` | **405 Method Not Allowed** | Spring MVC routing | Route có nhưng sai method (GET/POST/PUT...) |
| `ConstraintViolationException` | **400 Bad Request** | Jakarta Validation (`@Validated` trên Controller) | Lỗi validate `@RequestParam` / `@PathVariable` (vd: `@Min(1)`) |
| `HandlerMethodValidationException` | **400 Bad Request** | Spring 6.1+ Built-in Method Validation | Validate tham số method Controller của Spring Boot 3.2+ |
| `MethodArgumentTypeMismatchException` | **400 Bad Request** | Spring MVC binding | Path/query param sai kiểu (UUID vs String) |
| `HttpMessageNotReadableException` | **400 Bad Request** | Jackson deserialization | JSON body sai cú pháp (≠ lỗi `@Valid` của body) |
| `MaxUploadSizeExceededException` | **413 Payload Too Large** | Spring Multipart | File upload vượt giới hạn cấu hình |

### Mẫu Response Body (JSON Envelope)

#### 1. Lỗi Concurrency (HTTP 409):
```json
{
  "code": 409,
  "message": "The resource was modified by another request. Please retry.",
  "data": null,
  "timestamp": 1725064560000
}
```

#### 2. Lỗi Route không tồn tại (HTTP 404):
```json
{
  "code": 404,
  "message": "The requested resource was not found: /api/products/unknown-route",
  "data": null,
  "timestamp": 1725064560000
}
```

#### 3. Lỗi Validate Tham Số / Query Param (HTTP 400):
```json
{
  "code": 400,
  "message": "Invalid request parameters",
  "data": [
    {
      "field": "page",
      "rejectedValue": 0,
      "message": "must be greater than or equal to 1"
    }
  ],
  "timestamp": 1725064560000
}
```

---

## 🛡️ 3. Pagination Guard — Chống DB Overload

### Cơ Chế Hoạt Động (Customizer Pattern)
Để đảm bảo an toàn 100% và không gây xung đột với Spring Boot, `PaginationGuardConfiguration` sử dụng bean:
```java
@Bean
public PageableHandlerMethodArgumentResolverCustomizer serviceKitPageableCustomizer(WebProperties webProperties) {
    return resolver -> resolver.setMaxPageSize(webProperties.getPagination().getMaxPageSize());
}
```
`SpringDataWebAutoConfiguration` của Spring Boot tự động thu thập bean customizer này và áp dụng `setMaxPageSize()` lên `PageableHandlerMethodArgumentResolver` mặc định mà **không đè hay thay thế bean gốc**, đảm bảo an toàn tuyệt đối bất kể thứ tự khởi tạo bean.

### Cấu Hình & Hiệu Ứng Thực Tế

Cấu hình trong `application.yml`:
```yaml
service-kit:
  web:
    pagination:
      max-page-size: 200  # Mặc định: 100 nếu không khai báo
```

**Hành vi khi vượt giới hạn**: Spring Data tự động clamp xuống `maxPageSize` đã cấu hình — **không ném lỗi**, giúp truy vấn DB an toàn:

```
# Client gửi query quá tải:
GET /api/products?page=0&size=999999

# Hệ thống tự động giới hạn theo max-page-size (vd: 200):
pageSize = min(999999, 200) = 200  →  SELECT * FROM products LIMIT 200
```

---

## 💻 4. Ví Dụ Thực Chiến End-to-End

### 4.1 Kịch Bản Optimistic Lock Conflict (Race Condition)

**Entity** kế thừa `VersionedSoftDeletableEntity` từ `service-kit-data`:
```java
@Entity
@Table(name = "products")
@Getter @Setter
public class ProductEntity extends VersionedSoftDeletableEntity {
    private String name;
    private Integer stock;
}
```

**Luồng Race Condition** — Request A và B cùng đọc entity version=1:
```
Request A (Thread-1)                    Request B (Thread-2)
   │ Read: version=1, stock=10             │
   │                                       │ Read: version=1, stock=10
   │ Update: stock=15                      │
   │                                       │ Update: stock=20
   │ Commit → UPDATE SET stock=15          │
   │          WHERE version=1              │
   │ → 1 row updated ✅                    │
   │                                       │ Commit → UPDATE SET stock=20
   │                                       │          WHERE version=1
   │                                       │ → 0 rows updated ❌ (version đã = 2)
   │                                       │ → ObjectOptimisticLockingFailureException
   │                                       │
   ▼                                       ▼ [WebExceptionHandler bắt lỗi]
HTTP 200 OK                             HTTP 409 Conflict
                                        {"code":409,"message":"...retry."}
```

### 4.2 Kịch Bản Client Gửi Tham Số Không Hợp Lệ

#### Case 1: Sai kiểu dữ liệu (Type Mismatch)
```
# Client gọi:
GET /api/products/not-a-valid-uuid

# WebExceptionHandler bắt MethodArgumentTypeMismatchException:
HTTP 400 Bad Request
{
  "code": 400,
  "message": "Parameter 'id' must be of type 'UUID', but received: 'not-a-valid-uuid'",
  "data": null,
  "timestamp": 1725064560000
}
```

#### Case 2: Vi phạm constraint (@Min(1) @RequestParam page)
```
# Client gọi:
GET /api/products?page=0

# WebExceptionHandler bắt ConstraintViolationException / HandlerMethodValidationException:
HTTP 400 Bad Request
{
  "code": 400,
  "message": "Invalid request parameters",
  "data": [
    {"field": "page", "rejectedValue": 0, "message": "must be greater than or equal to 1"}
  ],
  "timestamp": 1725064560000
}
```

---

## ⚠️ 5. Lưu Ý Thực Tế & Góc Khuất (Edge Cases & Gotchas)

| # | Vấn Đề | Tác Động | Giải Pháp Chuẩn |
|---|---|---|---|
| **1** | **Thời Điểm Flush Của Hibernate (Flush-Timing)** | Nếu `@Transactional` gọi `repository.save()` mà Hibernate defer flush đến commit (`JpaTransactionManager.doCommit()`), lỗi version-conflict có thể bị bọc trong `TransactionSystemException` và lọt qua handler 409, rơi vào catch-all 500. | Viết concurrent integration test (2 threads cùng update 1 entity) và in ra `ex.getClass()` để xác nhận handler bắt đúng. Nếu cần nổ lỗi ngay lập tức tại service, dùng `.saveAndFlush(entity)`. |
| **2** | **Tránh Đụng Độ `@Order` Giữa Các Module** | Nếu nhiều `@RestControllerAdvice` cùng khai báo cùng một `@Order`, thứ tự xử lý exception giữa chúng sẽ không xác định. | Tuân thủ nghiêm ngặt bảng quy ước: **Security = 10**, **Web = 20**, **Common = LOWEST_PRECEDENCE**. |
| **3** | **Pagination Guard chỉ `clamp`, không báo lỗi** | Client gửi `size=999999` sẽ nhận về đúng số lượng tối đa (`maxPageSize`) nhưng không nhận thông báo lỗi. | Chấp nhận hành vi này vì đây là design cố ý (fail-safe). Nếu cần thông báo rõ, có thể gắn thêm response header tùy chọn trong tương lai. |

---

## 🔲 6. Roadmap

- [ ] **`IdempotencyFilter`**: Chống gọi API trùng lặp (dùng kèm `service-kit-redis`). Quan trọng cho API tạo đơn, thanh toán.
- [ ] **`RateLimitFilter`**: Chặn spam request theo IP hoặc User ID.
- [ ] **CORS Configuration**: Cấu hình `allowed-origins` linh hoạt theo từng service (public-facing vs internal). Cần cấu hình riêng vì `allowed-origins` khác nhau tùy service.
- [ ] **Security Headers**: Tự động inject static security headers vào HTTP Response (`X-Frame-Options`, `X-Content-Type-Options`, `Strict-Transport-Security`). Áp dụng đồng nhất mọi service, không cần cấu hình theo từng service.
- [ ] **`service-kit-client` (module mới)**: Chuẩn hóa lỗi outbound HTTP (timeout, connection refused, lỗi 4xx/5xx từ service khác → parse thành exception nội bộ có ý nghĩa).
