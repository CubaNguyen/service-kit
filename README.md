# Service Kit — Notes & Roadmap

Ghi chú nội bộ cho quá trình xây dựng bộ thư viện `service-kit`.

---

## ✅ Modules Đã Xây Dựng

| Module | Trạng thái | Ghi chú |
|---|---|---|
| `service-kit-dependencies` | ✅ Done | BOM quản lý version |
| `service-kit-common` | ✅ Done | Exception, Response, Filter, TimeUtils, Validation |
| `service-kit-data` | ✅ Done | Entity Hierarchy (UUID), Soft Delete, BaseRepository, HikariCP, Envers, Encryption |
| `service-kit-security` | 🔲 Skeleton | Cần triển khai JwtFilter, TokenProvider, SecurityAutoConfig |
| `service-kit-multitenant` | 🔲 Skeleton | Cần triển khai TenantFilter, MultiTenantAutoConfig |

---

## 🔲 TODO — Modules Chưa Xây Dựng

| Module | Mục đích | Cần làm / Chức năng chính |
|---|---|---|
| `service-kit-outbox` | Giải quyết Dual-Write Problem khi ghi DB & publish event | `OutboxEvent` Entity (kế thừa `BaseEntity`), Polling Scheduler, Retry mechanism, Debezium CDC |
| `service-kit-web` | Xử lý HTTP layer (những tính năng cần `spring-web`) | `GlobalWebExceptionHandler` (Map OptimisticLock → HTTP 409), `IdempotencyFilter` |
| `service-kit-redis` | Tầng Cache & Distributed Lock (Gộp chung) | Base Cache Service (get/set TTL, JSON Serializer), RedisAutoConfig, `@DistributedLock` (Redisson), Idempotency key |
| `service-kit-eventbus` | Giao tiếp Event Driven (Gộp chung) | Chứa `IEventBus`, `DomainEvent` nội bộ, và marker interfaces. Tự động quét đăng ký Listener. Hỗ trợ Kafka/RabbitMQ tùy chọn |
| `service-kit-spring-boot-starter`| Tầng Đóng gói "All-in-one" tiện lợi nhất | File cấu hình `auto-configuration` để tự động móc nối toàn bộ hệ sinh thái khi import |

---

## ⚠️ Lưu Ý Kiến Trúc (Không Code Ở Đây)

- **Expand-Contract Migration:** Quy trình DevOps/Process — viết guidelines vào Wiki/Confluence của team, không phải code component
- **Idempotency Key:** Nếu lưu bằng DB table → entity ở `service-kit-data`; logic Filter/Interceptor → `service-kit-web` hoặc `service-kit-redis`
- **OptimisticLockException:** Exception ném ra từ `service-kit-data`; mapping sang HTTP 409 phải đặt ở `service-kit-web` (tránh phụ thuộc `spring-web` ở Data layer)
- **Outbox trong Multi-Tenant:** `OutboxEvent` cần kế thừa `TenantEntity` (từ `service-kit-multitenant`) để tự động gán `tenant_id`
