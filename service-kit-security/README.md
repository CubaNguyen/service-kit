# Service Kit — Security Module 🔐

Module `service-kit-security` cung cấp bộ giải pháp bảo mật và quản lý ngữ cảnh người dùng (**Authentication & Authorization**) dạng không trạng thái (stateless) linh hoạt. Thư viện được thiết kế theo mô hình trừu tượng hóa phương thức cung cấp Token (Token Provider Strategy) cho phép dễ dàng mở rộng và hỗ trợ nhiều định dạng token khác nhau (như **JWT**, **PASETO**, hoặc các loại Opaque Token khác), trong đó mặc định tích hợp sẵn cấu hình JWT ký số bất đối xứng (RS256).

---

## 📑 Mục Lục (Table of Contents)
- [📌 1. Tổng Quan & Cài Đặt](#-1-tổng-quan--cài-đặt)
  - [Chế độ Hoạt Động (Auth Mode)](#chế-độ-hoạt-động-auth-mode)
  - [Cài Đặt (Installation)](#cài-đặt-installation)
- [👤 2. Ngữ Cảnh Người Dùng (AuthContext)](#-2-ngữ-cảnh-người-dùng-authcontext)
  - [Cấu trúc AuthContext](#cấu-trúc-authcontext)
  - [Nguồn gốc của Roles & Permissions](#nguồn-gốc-của-roles--permissions)
- [⚙️ 3. Cấu Hình Hệ Thống (Configuration)](#️-3-cấu-hình-hệ-thống-configuration)
  - [Bảng tham số cấu hình](#bảng-tham-số-cấu-hình)
- [🛡️ 4. HTTP Security Filters](#️-4-http-security-filters)
  - [TokenAuthenticationFilter & Chống rò rỉ bộ nhớ](#tokenauthenticationfilter--chống-rò-rỉ-bộ-nhớ)
  - [SecurityHeadersFilter (HTTP Security Headers)](#securityheadersfilter-http-security-headers)
- [🎟️ 5. Token Revocation Store — Cơ Chế Thu Hồi 2 Lớp](#️-5-token-revocation-store--cơ-chế-thu-hồi-2-lớp)
  - [Cơ chế check thu hồi](#cơ-chế-check-thu-hồi)
  - [Các loại Revocation Store hỗ trợ](#các-loại-revocation-store-hỗ-trợ)
- [💎 6. TokenClaimsCustomizer — Tùy Biến Claims Token](#-6-tokenclaimscustomizer--tùy-biến-claims-token)
- [🏷️ 7. Phân Quyền Bằng Annotation (AOP)](#️-7-phân-quyền-bằng-annotation-aop)
  - [Vai trò hệ thống vs Quyền hạn chi tiết](#vai-trò-hệ-thống-vs-quyền-hạn-chi-tiết)
  - [@RequireRole & @RequirePermission](#requirerole--requirepermission)
- [📡 8. Lan Truyền Ngữ Cảnh (Context Propagation)](#-8-lan-truyền-ngữ-cảnh-context-propagation)
  - [Feign Client Token Propagation](#feign-client-token-propagation)
  - [Async Execution (@Async Context Copying)](#async-execution-async-context-copying)

---

## 📌 1. Tổng Quan & Cài Đặt

### Chế độ Hoạt Động (Auth Mode)

Hỗ trợ 2 chế độ hoạt động chính giúp tối ưu hóa bảo mật và tài nguyên hệ thống:

1. **`FULL`**:
   - Dành cho các **Identity Service** (Auth Service, Gateway).
   - Có quyền ký và phát sinh token mới (Yêu cầu cả **RSA Private Key** để ký và **RSA Public Key** để kiểm tra).
2. **`VERIFY_ONLY`**:
   - Dành cho các **Downstream API Service**.
   - Chỉ có quyền giải mã và kiểm chứng token (Chỉ yêu cầu **RSA Public Key**, không được giữ Private Key).
   - Mọi nỗ lực gọi phát sinh token (`generateToken`) ở chế độ này sẽ ném ra `UnsupportedOperationException`.

### Cài Đặt (Installation)

Thêm dependency vào `pom.xml` của service:

```xml
<dependency>
    <groupId>com.servicekit</groupId>
    <artifactId>service-kit-security</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

---

## 👤 2. Ngữ Cảnh Người Dùng (AuthContext)

### Cấu trúc AuthContext

Khi xác thực thành công, toàn bộ thông tin của user được lưu trữ trong ThreadLocal qua lớp `AuthContextHolder`. Đối tượng `AuthContext` có cấu trúc cụ thể như sau:

```java
public record AuthContext(
        UUID userId,           // ID duy nhất của người dùng
        String tenantId,       // ID của Tenant (nullable - tự động gán nếu dùng Multi-Tenant)
        Set<String> roles,     // Danh sách vai trò (ví dụ: USER, ADMIN, SYSTEM_ADMIN)
        Set<String> permissions,// Danh sách quyền hạn chi tiết (ví dụ: product:write)
        String jti,            // JWT ID (ID duy nhất của Token hiện tại, bắt buộc cho revocation)
        Instant issuedAt       // Mốc thời gian phát hành token (bắt buộc cho global cutoff check)
) {}
```

Bạn có thể dễ dàng lấy thông tin user hiện tại ở bất kỳ đâu trong cùng một request thread bằng cách gọi:
```java
AuthContext userContext = AuthContextHolder.getContext();
UUID userId = userContext.userId();
```

### Nguồn gốc của Roles & Permissions

> [!NOTE]
> Để đảm bảo tính **Stateless (không trạng thái)** và tối ưu hiệu năng (không cần truy cập Database ở mỗi request để kiểm tra quyền), cả **Roles** và **Permissions** đều được **nhúng trực tiếp vào token claims** (`roles` và `permissions` claims) trong quá trình phát sinh token ở Auth/Identity Service. Khi gọi API, downstream service chỉ việc giải mã token và đọc trực tiếp các quyền từ token này.

---

## ⚙️ 3. Cấu Hình Hệ Thống (Configuration)

### Bảng tham số cấu hình

Khai báo các tham số sau trong `application.yml`:

```yaml
service-kit:
  security:
    auth-mode: VERIFY_ONLY            # FULL | VERIFY_ONLY (Mặc định: VERIFY_ONLY)
    token-type: JWT                   # JWT | PASETO (PASETO hiện tại thuộc Roadmap và chưa được kích hoạt)
    revocation-store: NONE            # NONE | IN_MEMORY | REDIS (Mặc định: NONE)
    jwt:
      public-key-path: classpath:keys/public.pem   # Bắt buộc cho cả 2 mode (khi token-type=JWT)
      private-key-path: classpath:keys/private.pem # Chỉ bắt buộc khi chạy FULL mode (khi token-type=JWT)
      algorithm: RS256                             # Thuật toán mã hóa (Mặc định: RS256)
      expiration-seconds: 3600                     # TTL của token (Mặc định: 3600 giây)
    permit-all-urls:                  # Danh sách API trắng không cần token
      - /api/v1/auth/login
      - /api/v1/public/**
```

---

## 🛡️ 4. HTTP Security Filters

### TokenAuthenticationFilter & Chống rò rỉ bộ nhớ

`TokenAuthenticationFilter` phụ trách trích xuất Bearer Token từ request, giải mã thông tin gán vào `AuthContextHolder` và tích hợp bối cảnh bảo mật của Spring Security.

> [!WARNING]
> Do Tomcat (và các Servlet engines) hoạt động theo cơ chế **Thread Pooling** (tái sử dụng các luồng cũ để xử lý request mới), việc quên dọn dẹp ThreadLocal sẽ làm lộ thông tin bảo mật của user trước cho request sau. 
> `TokenAuthenticationFilter` bảo vệ ứng dụng bằng cách bắt buộc dọn dẹp context trong khối `finally`:
> ```java
> try {
>     filterChain.doFilter(request, response);
> } finally {
>     AuthContextHolder.clearContext(); // Bảo đảm xóa sạch ThreadLocal
> }
> ```

### SecurityHeadersFilter (HTTP Security Headers)

`SecurityHeadersFilter` tự động tiêm các HTTP Header bảo mật tiêu chuẩn vào mọi Response của hệ thống nhằm giảm thiểu các nguy cơ tấn công Web cơ bản:

- **`X-Frame-Options: DENY`**: Chống clickjacking.
- **`X-Content-Type-Options: nosniff`**: Chặn MIME-sniffing.
- **`X-XSS-Protection: 1; mode=block`**: Kích hoạt bộ lọc XSS của trình duyệt.
- **`Strict-Transport-Security` (HSTS)**: Ép buộc kết nối HTTPS.
- **`Content-Security-Policy` (CSP)**: Định nghĩa tài nguyên an toàn (`default-src 'self'`).

---

## 🎟️ 5. Token Revocation Store — Cơ Chế Thu Hồi 2 Lớp

### Cơ chế check thu hồi

Khi kiểm tra một request đi vào qua `TokenAuthenticationFilter`, hệ thống thực thi kiểm tra thu hồi qua **2 lớp bảo vệ**:

1. **Lớp 1 — Hủy thiết bị/phiên cụ thể (Blacklist `jti`)**:
   - Sử dụng JWT ID (`jti`) duy nhất của token. Khi user bấm đăng xuất trên thiết bị hiện tại, `jti` của token đó sẽ bị đẩy vào blacklist của store với TTL bằng thời gian sống còn lại của token.
2. **Lớp 2 — Hủy toàn bộ phiên (Cutoff Timestamp)**:
   - Khi user đổi mật khẩu hoặc bấm "đăng xuất khỏi tất cả các thiết bị", hệ thống sẽ lưu mốc thời gian hủy (`cutoff`) cho `userId` đó. Bất kỳ token nào có thời gian phát hành (`issuedAt`) trước mốc `cutoff` này sẽ lập tức bị coi là vô hiệu, bất kể `jti` của nó là gì.

```java
boolean isRevoked = revocationStore.isTokenRevoked(authContext.jti())
        || (revocationStore.getRevokeAllCutoff(authContext.userId()) != null
            && authContext.issuedAt().isBefore(revocationStore.getRevokeAllCutoff(authContext.userId())));
```

### Các loại Revocation Store hỗ trợ

Cấu hình qua `service-kit.security.revocation-store`:

- **`NONE`**: Mặc định. Nạp `NoOpRevocationStore`, các lệnh check thu hồi luôn trả về `false`, tốn chi phí I/O gần như bằng 0.
- **`IN_MEMORY`**: Nạp `InMemoryRevocationStore` sử dụng `ConcurrentHashMap` tự động dọn dẹp token hết hạn. Phù hợp cho dev/test hoặc monolith đơn instance.
- **`REDIS`**: Nạp `RedisRevocationStore` chia sẻ trạng thái giữa các cụm microservices. **Chỉ tự động cấu hình** khi thuộc tính được set là `REDIS` và dự án chủ động khai báo `spring-boot-starter-data-redis` trên classpath.

---

## 💎 6. TokenClaimsCustomizer — Tùy Biến Claims Token

Nếu bạn cần lưu trữ thêm các thông tin nghiệp vụ tùy biến khác bên trong token (ví dụ: `storeId`, `email`, `branchCode`), chỉ cần khai báo một Spring bean triển khai interface `TokenClaimsCustomizer`:

```java
@Component
public class UserEmailClaimsCustomizer implements TokenClaimsCustomizer {
    
    @Override
    public void customize(Map<String, Object> claims, AuthContext context) {
        // Trích xuất thông tin nghiệp vụ và đưa vào claims map
        claims.put("email", "user@company.com");
        claims.put("storeId", 12345);
    }
}
```
`JwtTokenProvider` sẽ tự động phát hiện tất cả các Customizer trong ApplicationContext và đưa thông tin vào token trong quá trình sinh token (`generateToken`).

---

## 🏷️ 7. Phân Quyền Bằng Annotation (AOP)

### Vai trò hệ thống vs Quyền hạn chi tiết

Hệ thống phân biệt rõ hai khái niệm phân quyền:
1. **Vai trò hệ thống (Roles)**: Cho biết người dùng thuộc nhóm người dùng nào (Ví dụ: `USER`, `ADMIN`, `SYSTEM_ADMIN`). Được bảo vệ thông qua `@RequireRole`.
2. **Quyền hạn hành động (Permissions)**: Ràng buộc hành động nghiệp vụ cụ thể (Ví dụ: `product:write`, `product:read`). Được bảo vệ thông qua `@RequirePermission`.

### @RequireRole & @RequirePermission

Bạn có thể phân quyền truy cập trực tiếp trên method hoặc class của Controller/Service bằng các annotation:

```java
@RestController
@RequestMapping("/api/v1/products")
@RequireRole({"ADMIN", "SYSTEM_ADMIN"}) // Yêu cầu vai trò hệ thống ở mức Class
public class ProductController {

    @PostMapping
    @RequirePermission("product:write") // Yêu cầu quyền hành động chi tiết ở mức Method
    public ApiResponse<Void> createProduct() {
        return ApiResponse.success(null);
    }
}
```

Nếu vi phạm phân quyền, Aspect sẽ tự động ném ra `ForbiddenException` (sẽ được map về HTTP 403 ở tầng Web) hoặc `UnauthorizedException` (HTTP 401) nếu chưa đăng nhập.

> [!TIP]
> **Best Practice: Sử dụng Enum/Constant để tránh lỗi chính tả (Typo)**
> Do cấu trúc ngôn ngữ Java chỉ cho phép truyền hằng số compile-time tĩnh vào Annotation (không cho phép gọi hàm như `.name()`), lập trình viên tích hợp module này có thể xây dựng các Enum vai trò kèm theo định nghĩa `Fields` tĩnh để sử dụng an toàn (Type-safe) như sau:
>
> ```java
> public enum UserRole {
>     ADMIN(Fields.ADMIN),
>     USER(Fields.USER);
>
>     private final String value;
> 
>     UserRole(String value) { 
>         this.value = value; 
>     }
>
>     public static class Fields {
>         public static final String ADMIN = "ADMIN";
>         public static final String USER = "USER";
>     }
> }
> ```
> 
> Khi gán quyền tại Controller, chỉ cần truyền biến hằng số lớp `Fields` tĩnh:
> ```java
> @RequireRole(UserRole.Fields.ADMIN)
> ```
> Cách tiếp cận này giúp bạn vừa có Enum để lưu xuống Database (sử dụng `@Enumerated(EnumType.STRING)` của JPA), vừa đảm bảo an toàn kiểu dữ liệu compile-time khi dùng với `@RequireRole`.

### Quy tắc Đối Khớp (Semantics & Matching Rules)

Để tránh hiểu nhầm khi áp dụng phân quyền, hệ thống hoạt động chặt chẽ theo các quy tắc logic sau:

1. **Logic `OR` đối với nhiều tham số trong cùng 1 annotation**:
   - Khi khai báo `@RequireRole({"ADMIN", "SYSTEM_ADMIN"})` hoặc `@RequirePermission({"product:write", "product:delete"})`, người dùng chỉ cần sở hữu **ít nhất một** trong các vai trò/quyền hạn được liệt kê là có thể truy cập thành công.
2. **Logic `AND` khi kết hợp các loại annotation khác nhau**:
   - Khi kết hợp `@RequireRole` ở mức Class và `@RequirePermission` ở mức Method, người dùng **phải thỏa mãn đồng thời cả hai điều kiện** (phải có vai trò được yêu cầu ở Class **VÀ** có quyền hạn chi tiết yêu cầu ở Method).
3. **Logic `Ghi Đè (Override)` khi trùng loại annotation**:
   - Nếu `@RequireRole` (hoặc `@RequirePermission`) xuất hiện ở cả mức Class và Method, annotation ở mức **Method sẽ ghi đè hoàn toàn** annotation ở mức Class.
   - *Ví dụ:* Class đánh dấu `@RequireRole("ADMIN")` nhưng Method bên trong đánh dấu `@RequireRole("USER")`, thì tại method đó hệ thống chỉ kiểm tra vai trò `USER` (ADMIN không có role USER sẽ bị chặn).

---

## 📡 8. Lan Truyền Ngữ Cảnh (Context Propagation)

### Feign Client Token Propagation
Khi một service gọi HTTP call sang service khác qua Feign Client, `FeignAuthInterceptor` sẽ tự động trích xuất Bearer Token từ request thread hiện tại và đính kèm vào HTTP Header `Authorization` của outgoing request:
```
[Client] ---> Authorization: Bearer <token> ---> [Gateway]
                                                      │ (Tự động chuyển tiếp token)
                                                      ▼
                                                 [Service A] ---> Authorization: Bearer <token> ---> [Service B]
```

### Async Execution (@Async Context Copying)
Vì `AuthContextHolder` sử dụng `ThreadLocal`, bối cảnh bảo mật sẽ bị mất khi chạy các method bất đồng bộ `@Async` (do chạy trên một Thread pool khác). 
Để giải quyết việc này, module cung cấp `AsyncContextPropagator` (triển khai `TaskDecorator`). Bạn chỉ cần cấu hình TaskExecutor để sử dụng decorator này:

```java
@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean
    public Executor taskExecutor(AsyncContextPropagator contextPropagator) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setTaskDecorator(contextPropagator); // Tự động copy context sang thread con
        executor.initialize();
        return executor;
    }
}
```
