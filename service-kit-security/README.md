# Service Kit Security

`service-kit-security` là module quản lý **Bảo mật, Xác thực (Authentication), Phân quyền (Authorization) và Ngữ cảnh Người dùng (User/Auth Context)** dành cho hệ sinh thái Microservices xây dựng bằng **Spring Boot 3.4.x** và **Spring Security 6.x**.

---

## 🎯 Tổng Quan Mục Đích & Roadmap Triển Khai

Module này được thiết kế để giải quyết bài toán:
1. **Xác thực Token tập trung**: Hỗ trợ parse JWT (JSON Web Token) hoặc Paseto Token từ header `Authorization: Bearer <token>`.
2. **Bóc tách User Context tự động**: Trích xuất `UserId`, `Email`, `Role`, `Permissions`, `Tenant/SiteKey`, `Client IP`, `User-Agent`, `Platform` và nạp vào `AuthContextHolder` (ThreadLocal).
3. **Phân quyền linh hoạt**: Tích hợp với `@PreAuthorize("hasRole('ADMIN')")` hoặc `@PreAuthorize("hasAuthority('ORDER_READ')")`.
4. **Tích hợp JPA Auditing**: Cung cấp `SpringSecurityAuditorAware` để tự động gán `createdBy` / `updatedBy` vào Database Entity mà không cần dev phải set tay.
5. **Đẩy thông tin Log (SLF4J MDC)**: Tự động nhúng `userId`, `email`, `ipAddress` vào log pattern để tracking thao tác của người dùng.

---

## 🏗️ Cấu Trúc Thư Mục Đề Xuất (Architecture Blueprint)

```
service-kit-security/
├── src/main/java/com/servicekit/security/
│   ├── config/
│   │   ├── SecurityAutoConfiguration.java   # Cấu hình SecurityFilterChain mặc định (Stateless, CSRF Disable, CORS)
│   │   └── SecurityProperties.java          # @ConfigurationProperties("service-kit.security") (public-endpoints, secret-key...)
│   ├── context/
│   │   ├── AuthContext.java                 # DTO chứa toàn bộ metadata của phiên đăng nhập
│   │   └── AuthContextHolder.java           # ThreadLocal wrapper quản lý AuthContext xuyên suốt request
│   ├── filter/
│   │   └── JwtAuthenticationFilter.java     # Filter bóc tách Bearer Token, nạp Authentication & AuthContext
│   ├── auditor/
│   │   └── SpringSecurityAuditorAware.java  # Implement AuditorAware<String> lấy UserId từ AuthContext
│   ├── token/
│   │   ├── TokenProvider.java               # Interface giải mã token (JWT / Paseto)
│   │   └── JwtTokenProvider.java            # Triển khai giải mã JWT bằng Nimbus / JJWT
│   └── util/
│       └── SecurityUtils.java               # Helper lấy currentUserId(), hasRole(), isAuthenticated()...
└── README.md
```

---

## 🚀 Hướng Dẫn Thiết Kế & Triển Khai Chi Tiết (Step-by-Step Guide)

### 1. `AuthContext` & `AuthContextHolder` (Đã khởi tạo sẵn khung)
- **`AuthContext`**: Chứa toàn bộ thông tin người dùng được giải mã từ Token và Request Header.
- **`AuthContextHolder`**: Quản lý `ThreadLocal<AuthContext>` an toàn, có phương thức `clear()` để dọn dẹp trong `finally` tránh rò rỉ Thread Pool.

```java
// Cách sử dụng ở bất kỳ tầng nào (Service, Controller, Component):
String currentUserId = AuthContextHolder.getUserId();
String currentUserEmail = AuthContextHolder.getEmail();
AuthContext context = AuthContextHolder.getContext();
```

---

### 2. Triển Khai `JwtAuthenticationFilter` (Mẫu tham khảo)

Filter này chặn trước mọi request, đọc header `Authorization` và các header phụ trợ:

```java
package com.servicekit.security.filter;

import com.servicekit.security.context.AuthContext;
import com.servicekit.security.context.AuthContextHolder;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.slf4j.MDC;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            
            // TODO: Giải mã token lấy claims (UserId, Email, Roles...)
            String userId = "12345"; // Trích xuất từ Token
            String email = "john@example.com";
            List<String> roles = List.of("ROLE_USER");

            // 1. Tạo AuthContext
            AuthContext authContext = AuthContext.builder()
                    .userId(userId)
                    .email(email)
                    .roles(roles)
                    .token(token)
                    .ipAddress(request.getRemoteAddr())
                    .userAgent(request.getHeader("User-Agent"))
                    .correlationId(request.getHeader("X-Correlation-Id"))
                    .build();

            AuthContextHolder.setContext(authContext);
            MDC.put("userId", userId);

            // 2. Nạp vào Spring SecurityContext
            var authorities = roles.stream().map(SimpleGrantedAuthority::new).toList();
            var authentication = new UsernamePasswordAuthenticationToken(userId, null, authorities);
            SecurityContextHolder.getContext().setAuthentication(authentication);
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            // [Quan trọng] Dọn dẹp bộ nhớ chống leak Thread Pool
            AuthContextHolder.clear();
            MDC.remove("userId");
        }
    }
}
```

---

### 3. Tích Hợp Tự Động Gán `createdBy` / `updatedBy` Với `service-kit-data`

Triển khai `AuditorAware<String>`:

```java
package com.servicekit.security.auditor;

import com.servicekit.security.context.AuthContextHolder;
import org.springframework.data.domain.AuditorAware;
import java.util.Optional;

public class SpringSecurityAuditorAware implements AuditorAware<String> {

    @Override
    public Optional<String> getCurrentAuditor() {
        String userId = AuthContextHolder.getUserId();
        return Optional.ofNullable(userId != null ? userId : "SYSTEM");
    }
}
```

*Khi bật `@EnableJpaAuditing`, các Entity có `@CreatedBy private String createdBy;` sẽ tự động nhận ID của người gọi request mà không cần code thêm.*

---

## ⚙️ Cấu Hình Mặc Định Trong `SecurityAutoConfiguration`

Cung cấp cấu hình Stateless mặc định cho Microservices:
- Tắt CSRF (`csrf.disable()`).
- Tắt Session (`SessionCreationPolicy.STATELESS`).
- Cho phép whitelist các endpoint public (Swagger, Actuator, Health check).
- Chặn các request còn lại yêu cầu `authenticated()`.

---

## 🗓️ TODO — Các Tính Năng Chưa Xây Dựng (Liên Quan Đến Security)

> Xem chi tiết thiết kế và lý do phân tách module tại **[Root README — TODO Section](../README.md#️-todo--các-module-chưa-được-xây-dựng)**.

### TODO: `service-kit-web` — HTTP 409 Conflict Cho OptimisticLockException

**Vấn đề:** Khi Entity dùng `@Version` (Optimistic Locking) bị xung đột cập nhật đồng thời, Hibernate ném `OptimisticLockingFailureException`. Spring Boot mặc định map nó về HTTP **500** — client (mobile app, frontend) không biết đây là lỗi có thể retry được.

**Tại sao KHÔNG đặt handler này ở `service-kit-security`?**
> `service-kit-security` chỉ xử lý Authentication/Authorization. HTTP exception mapping là trách nhiệm của tầng Web, nên thuộc `service-kit-web`.

```java
// TODO: Implement trong service-kit-web / GlobalWebExceptionHandler
@ExceptionHandler({
    OptimisticLockingFailureException.class,
    ObjectOptimisticLockingFailureException.class
})
public ResponseEntity<ApiResponse<Void>> handleOptimisticLock(OptimisticLockingFailureException ex) {
    // HTTP 409 Conflict — client biết cần tải lại dữ liệu và retry
    return ResponseEntity.status(HttpStatus.CONFLICT)
            .body(ApiResponse.error("CONFLICT",
                "Dữ liệu vừa được cập nhật bởi người dùng khác. Vui lòng tải lại và thử lại."));
}
```

### TODO: `service-kit-web` — Idempotency Key Filter

**Vấn đề:** Client retry request khi timeout/mất mạng → Server xử lý 2 lần → Tạo duplicate record (ví dụ: tạo 2 đơn hàng cho 1 lần nhấn nút).

**Giải pháp:** HTTP Filter kiểm tra header `Idempotency-Key` trước khi request đến Controller:

```java
// TODO: Implement trong service-kit-web / IdempotencyFilter
// Request phải gửi: Idempotency-Key: <uuid>
// Filter:
//   1. Kiểm tra key trong Redis / DB
//   2. Nếu đã tồn tại → trả về response cache cũ ngay lập tức (không xử lý lại)
//   3. Nếu chưa → cho request đi tiếp, cache response sau khi xử lý xong
```

**Tại sao KHÔNG đặt ở `service-kit-security`?**
> Idempotency là vấn đề của tầng HTTP/Application, không phải Auth. Logic kiểm tra key có thể dùng Redis (→ `service-kit-redis`) hoặc DB table (entity ở `service-kit-data`).
