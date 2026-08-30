# Service Kit Common

`service-kit-common` là một module thư viện nền tảng (core library) dành cho các microservices xây dựng bằng **Java 21** và **Spring Boot 3.4.x**. Thư viện này cung cấp các tiêu chuẩn chung và tiện ích thiết yếu nhằm đảm bảo tính nhất quán, dễ bảo trì và dễ tích hợp giữa các services trong hệ thống.

## 🎯 Tổng Quan Mục Đích

Thư viện được thiết kế để giải quyết các vấn đề lặp đi lặp lại trong quá trình phát triển microservice:
- **Chuẩn hóa Response**: Tự động bọc mọi kết quả trả về từ Controller vào một định dạng chung (`ApiResponse<T>`).
- **Xử lý Lỗi Tập Trung**: Bắt và xử lý mọi Exception toàn cục, ánh xạ về các mã lỗi (`ErrorCode`) thống nhất.
- **Tracing & Logging**: Tự động sinh hoặc kế thừa `TraceId`, đưa vào `MDC` (Mapped Diagnostic Context) để tracking log xuyên suốt các service.
- **Xử lý Thời Gian**: Quản lý thời gian chuẩn UTC (13 chữ số - Epoch Milliseconds) thay vì các format ngày tháng phức tạp.
- **Cấu hình Jackson & JSON**: Chuẩn hóa việc parse và serialize JSON, tối ưu hóa payload và tương thích ngược.

---

## 📦 Cài Đặt (Installation)

Thêm dependency sau vào file `pom.xml` của service của bạn:

```xml
<dependency>
    <groupId>com.servicekit</groupId>
    <artifactId>service-kit-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

**Tách lỏng phụ thuộc Web (Dành cho service dạng API)**:
Để các service dạng Worker/Consumer không bị kéo theo Tomcat Server khi import module `common`, các dependency liên quan đến Web và Validation đã được gắn thẻ `<optional>true</optional>`. 

Nếu service của bạn là ứng dụng **Web / REST API**, hãy chắc chắn rằng bạn có khai báo các thư viện này trong `pom.xml`:

```xml
<!-- Validation API & Hibernate Validator -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<!-- Web Stack (Controller, Advice, Filter) -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>
```

> **Lưu ý**: Module đã sử dụng cơ chế Auto-Configuration của Spring Boot (`org.springframework.boot.autoconfigure.AutoConfiguration.imports`), do đó bạn không cần phải khai báo `@ComponentScan` thủ công cho các package của thư viện này.

---

## 🚀 Hướng Dẫn Sử Dụng (How-to-use)

### Bảng Tra Cứu Nhanh

| Tính năng | Class/Annotation | Mục đích |
|---|---|---|
| Chuẩn hóa Response | `ApiResponse<T>` | Định dạng chuẩn trả về cho client. |
| Phân trang | `PageResponse<T>` | Object bọc dữ liệu danh sách phân trang. |
| Bọc Response tự động | `GlobalResponseBodyAdvice` | Tự động bọc dữ liệu trả về từ Controller thành `ApiResponse`. |
| Bỏ qua bọc Response | `@IgnoreResponseAdvice` | Dùng ở Controller/Method để trả về dữ liệu gốc. |
| Lỗi nghiệp vụ | `BaseBusinessException` | Lớp cha cho các custom exception có mã lỗi. |
| Chi tiết lỗi Validate | `FieldErrorDetail` | Trả về chi tiết các trường bị lỗi khi dùng `@Valid`. |
| Mã lỗi | `ErrorCode` | Enum định nghĩa mã lỗi và HTTP Status. |
| Bắt lỗi toàn cục | `GlobalExceptionHandler` | ControllerAdvice xử lý exception. |
| Validate Enum | `@ValidEnum` | Annotation kiểm tra giá trị String có thuộc Enum hợp lệ không. |
| Tracking Request | `TraceIdFilter` | Filter thêm `X-Correlation-Id` và gán vào MDC. |
| Auditing DTO/Entity | `IAuditable` | Interface chuẩn hóa khai báo trường thời gian (createdAt, updatedAt). |
| Tiện ích JSON | `JsonUtils` | Cung cấp hàm `toJson`, `fromJson` tiện lợi. |
| Tiện ích Thời gian | `TimeUtils` | Xử lý thời gian UTC, Epoch Milli. |
| Kiểu thời gian JSON | `UtcTimestamp` | Object bọc Epoch Milli cho request/response. |

---

### 1. Chuẩn Hóa Response & Tự Động Bọc Dữ Liệu

Mọi API trả về sẽ tự động được bọc trong class `ApiResponse<T>`.

**Sử dụng thông thường trong Controller:**
Bạn chỉ cần trả về Object, String hoặc Collection như bình thường. Thư viện sẽ tự bọc nó lại.

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        // Thay vì phải return ApiResponse.success(user), bạn chỉ cần return user
        return userService.getUserById(id); 
    }
}
```

**Kết quả trả về cho Client:**
```json
{
  "code": 200,
  "message": "Success",
  "data": {
    "id": 1,
    "name": "John Doe"
  },
  "timestamp": 1724916781123
}
```

**Sử dụng Phân Trang (`PageResponse`):**
Thay vì tự tính toán tổng số trang cho các API danh sách, bạn có thể dùng `PageResponse`:

```java
@GetMapping
public PageResponse<UserDto> getUsers(@RequestParam int page, @RequestParam int size) {
    List<UserDto> users = userService.getUsers(page, size);
    long total = userService.countUsers();
    
    // Tự động tính toán totalPages, hasNext, hasPrevious
    return PageResponse.of(users, page, size, total); 
}
```

Dữ liệu trả về sẽ được tự động bọc bởi `ApiResponse` (nằm trong block `data`), cung cấp đầy đủ thông tin phân trang cho client.

**Bypass (Bỏ qua) tự động bọc Response:**
Nếu bạn đang viết API cho đối tác thứ 3, hoặc export file, webhook... bạn có thể dùng `@IgnoreResponseAdvice` để trả về đúng nguyên bản dữ liệu.

```java
@GetMapping("/export")
@IgnoreResponseAdvice // Áp dụng cho method (hoặc áp dụng ở class)
public String exportCsv() {
    return "id,name\n1,John";
}
```

---

### 2. Quản Lý Exception

**Sử dụng ErrorCode định nghĩa sẵn:**
Thay vì ném `RuntimeException` chung chung, hãy sử dụng `ErrorCode` kết hợp với custom exception.

```java
public class NotFoundException extends BaseBusinessException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
```

**Ném lỗi trong Service:**
```java
@Service
public class UserService {
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }
}
```

`GlobalExceptionHandler` sẽ tự động bắt `NotFoundException`, log ở mức `WARN` (không in stacktrace để giảm nhiễu) và trả về Response tương ứng:

```json
{
  "code": 404,
  "message": "User not found with id: 1",
  "data": null,
  "timestamp": 1724916781500
}
```

> **Lưu ý Kiến Trúc**: Module tuân thủ **Semantic HTTP Status**. Thay vì trả về HTTP 200 OK và giấu lỗi bên trong body (một anti-pattern), `GlobalExceptionHandler` sử dụng `ResponseEntity` để trả về đúng chuẩn HTTP Status Code (400, 401, 403, 404, 500...) tương ứng với `ErrorCode`. Điều này rất quan trọng để hệ sinh thái Microservices (API Gateway, Load Balancer, Prometheus) không bị "mù" lỗi và có thể tracking/alerting chính xác.

**Xử Lý Lỗi Validate (`@Valid`):**
Khi dùng `@Valid` hoặc `@Validated` cho request payload và bị vi phạm (ví dụ thiếu field, sai format), hệ thống sẽ bắt `MethodArgumentNotValidException` và tự động bóc tách thành danh sách `FieldErrorDetail`:

```json
{
  "code": 400,
  "message": "Invalid request data",
  "data": [
    {
      "field": "email",
      "rejectedValue": "invalid-email",
      "message": "must be a well-formed email address"
    }
  ],
  "timestamp": 1724916781500
}
```

**Custom Annotation `@ValidEnum`:**
Thư viện cung cấp sẵn annotation `@ValidEnum` để tự động kiểm tra một chuỗi đầu vào (String) có khớp với giá trị của một Enum cho trước hay không (hỗ trợ `ignoreCase`).

```java
public class CreateOrderRequest {
    @ValidEnum(enumClass = OrderStatus.class, ignoreCase = true, message = "Status must be valid")
    private String status; 
}
```

Lỗi hệ thống bất ngờ (Exception chưa handle) sẽ tự động log `ERROR` kèm stacktrace đầy đủ và trả về lỗi 500.

---

### 3. Tracing Request (TraceId & MDC)

`TraceIdFilter` tự động chạy với độ ưu tiên cao nhất (`Ordered.HIGHEST_PRECEDENCE`).
- Nó kiểm tra header `X-Correlation-Id`. Nếu gọi chéo service (microservice này gọi microservice kia) đã có sẵn header, nó sẽ dùng lại.
- Nếu request gọi từ bên ngoài vào chưa có, nó sẽ tạo một `UUID` mới.
- TraceId này sẽ được đưa vào header của response trả về client, và được nạp vào `MDC` với key là `traceId`.

#### Cấu hình Logback (`logback-spring.xml`)
Để log của bạn hiển thị `traceId`, hãy thêm `%X{traceId}` vào log pattern. Cấu hình tại `src/main/resources/logback-spring.xml`:

```xml
<?xml version="1.0" encoding="UTF-8"?>
<configuration>
    <!-- Thêm %X{traceId} vào pattern -->
    <property name="LOG_PATTERN" value="%d{yyyy-MM-dd HH:mm:ss.SSS} [%thread] %-5level %logger{36} - [TraceId: %X{traceId}] - %msg%n" />

    <appender name="CONSOLE" class="ch.qos.logback.core.ConsoleAppender">
        <encoder>
            <pattern>${LOG_PATTERN}</pattern>
        </encoder>
    </appender>

    <root level="INFO">
        <appender-ref ref="CONSOLE" />
    </root>
</configuration>
```

**Log hiển thị thực tế:**
```text
2026-08-29 14:50:12.123 [http-nio-8080-exec-1] INFO  com.servicekit.UserService - [TraceId: 123e4567-e89b-12d3-a456-426614174000] - Getting user by id: 1
```

---

### 4. Xử Lý Thời Gian & JSON

Thư viện chuẩn hóa việc sử dụng thời gian dạng UTC Epoch Milliseconds (13 chữ số) để tránh lỗi parse Timezone.

**Chuẩn hóa Auditing DTO/Entity (`IAuditable`):**
Cung cấp một interface chung bắt buộc khai báo `createdAt` và `updatedAt` theo định dạng chuẩn Long (13 chữ số) cho các models.

```java
public class UserEntity implements IAuditable {
    private Long createdAt;
    private Long updatedAt;
    
    // Implement getters and setters...
}
```

**Sử dụng `TimeUtils`:**
```java
long now = TimeUtils.nowEpochMilli(); // 1724916781123
String iso8601 = TimeUtils.toUtcString(now); // 2026-08-29T07:33:01.123Z
```

**Sử dụng `UtcTimestamp` làm DTO:**
Sử dụng `UtcTimestamp` trong Request/Response DTO. Khi gọi API, client truyền số 13 chữ số, Jackson sẽ tự động map, và khi log nó sẽ hiển thị rất thân thiện:

```java
public class UserDto {
    private String name;
    private UtcTimestamp createdAt; // Jackson map to/from Long (epoch milli)
}

// Khi in log:
log.info("Created at: {}", dto.getCreatedAt()); 
// Result: Created at: 2026-08-29 14:35:51 UTC (1724916781123)
```

**Sử dụng `JsonUtils`:**
```java
String json = JsonUtils.toJson(new UserDto());
UserDto user = JsonUtils.fromJson(json, UserDto.class);

// Map generic type
List<UserDto> users = JsonUtils.fromJson("[{...}]", new TypeReference<List<UserDto>>() {});
```

---

## 🧪 Hướng Dẫn Viết Unit Test Nhanh

Ví dụ cách test API xem có được bọc `ApiResponse` và có trả về `X-Correlation-Id` đúng không bằng `@WebMvcTest`.

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.hamcrest.Matchers.*;

@WebMvcTest(controllers = UserController.class)
// Import các cấu hình auto của common để test
@Import({
    GlobalResponseBodyAdvice.class, 
    GlobalExceptionHandler.class, 
    TraceIdFilter.class, 
    ObjectMapperAutoConfiguration.class
})
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void testGetUser_ShouldWrapResponseAndIncludeTraceId() throws Exception {
        mockMvc.perform(get("/api/users/1")
                .header("X-Correlation-Id", "test-trace-123"))
                .andExpect(status().isOk())
                .andExpect(header().string("X-Correlation-Id", "test-trace-123"))
                .andExpect(jsonPath("$.code", is(200)))
                .andExpect(jsonPath("$.message", is("Success")))
                .andExpect(jsonPath("$.data.id", is(1)));
    }
    
    @Test
    void testGetUser_NotFound_ShouldReturnError() throws Exception {
        // Giả sử API /api/users/99 ném NotFoundException
        mockMvc.perform(get("/api/users/99"))
                // Kiểm tra trả về đúng HTTP Status 404 Not Found
                .andExpect(status().isNotFound()) 
                .andExpect(jsonPath("$.code", is(404)))
                .andExpect(jsonPath("$.message", containsString("not found")));
    }
}
```
