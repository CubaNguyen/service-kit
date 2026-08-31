# Service Kit Common

`service-kit-common` là một module thư viện nền tảng (core library) dành cho các microservices xây dựng bằng **Java 21** và **Spring Boot 3.4.x**. Thư viện này cung cấp các tiêu chuẩn chung và tiện ích thiết yếu nhằm đảm bảo tính nhất quán, dễ bảo trì và dễ tích hợp giữa các services trong hệ thống.

---

## 📑 Mục Lục (Table of Contents)

- [📌 1. Tổng Quan & Cài Đặt](#-1-tổng-quan--cài-đặt)
  - [Mục Đích Thư Viện](#mục-đích-thư-viện)
  - [Cài Đặt (Installation)](#cài-đặt-installation)
  - [Bảng Tra Cứu Nhanh (Quick Reference)](#bảng-tra-cứu-nhanh-quick-reference)
- [📖 2. Hướng Dẫn Sử Dụng Cơ Bản (Quick Start)](#-2-hướng-dẫn-sử-dụng-cơ-bản-quick-start)
  - [2.1 Chuẩn Hóa Response & Tự Động Bọc Dữ Liệu](#21-chuẩn-hóa-response--tự-động-bọc-dữ-liệu)
  - [2.2 Phân Trang Với PageResponse](#22-phân-trang-với-pageresponse)
  - [2.3 Quản Lý Exception & Xử Lý Lỗi Tập Trung](#23-quản-lý-exception--xử-lý-lỗi-tập-trung)
- [🔍 3. Tracing Request (TraceId & MDC)](#-3-tracing-request-traceid--mdc)
  - [Luồng Hoạt Động Của TraceIdFilter](#luồng-hoạt-động-của-traceidfilter)
  - [Cấu Hình Logback (logback-spring.xml)](#cấu-hình-logback-logback-springxml)
- [⚡ 4. Xử Lý Thời Gian & JSON (Utilities)](#-4-xử-lý-thời-gian--json-utilities)
  - [4.1 Chuẩn Hóa Auditing & TimeUtils](#41-chuẩn-hóa-auditing--timeutils)
  - [4.2 DTO UtcTimestamp & Jackson Customizer](#42-dto-utctimestamp--jackson-customizer)
  - [4.3 Tiện Ích JSON (JsonUtils)](#43-tiện-ích-json-jsonutils)
- [⚠️ 5. Lưu Ý Thực Tế & Góc Khuất (Edge Cases & Gotchas)](#️-5-lưu-ý-thực-tế--góc-khuất-edge-cases--gotchas)
- [🧪 6. Hướng Dẫn Viết Unit Test Nhanh](#-6-hướng-dẫn-viết-unit-test-nhanh)

---

## 📌 1. Tổng Quan & Cài Đặt

### Mục Đích Thư Viện

Thư viện được thiết kế để giải quyết các vấn đề lặp đi lặp lại trong quá trình phát triển microservice:
- **Chuẩn hóa Response**: Tự động bọc mọi kết quả trả về từ Controller vào một định dạng chung (`ApiResponse<T>`).
- **Xử lý Lỗi Tập Trung**: Bắt và xử lý mọi Exception toàn cục, ánh xạ về các mã lỗi (`ErrorCode`) thống nhất.
- **Tracing & Logging**: Tự động sinh hoặc kế thừa `TraceId`, đưa vào `MDC` (Mapped Diagnostic Context) để tracking log xuyên suốt các service.
- **Xử lý Thời Gian**: Quản lý thời gian chuẩn UTC (13 chữ số - Epoch Milliseconds) thay vị các format ngày tháng phức tạp.
- **Cấu hình Jackson & JSON**: Chuẩn hóa việc parse và serialize JSON, tối ưu hóa payload và tương thích ngược.

### Cài Đặt (Installation)

Thêm dependency sau vào file `pom.xml` của service của bạn:

```xml
<dependency>
    <groupId>com.servicekit</groupId>
    <artifactId>service-kit-common</artifactId>
    <version>1.0.0-SNAPSHOT</version>
</dependency>
```

#### Tách lỏng phụ thuộc Web (Dành cho service dạng API)
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

> [!NOTE]
> Thư viện đã tích hợp sẵn Spring Boot Auto-Configuration (`META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports`), do đó bạn **không cần** phải khai báo `@ComponentScan` thủ công cho các package của thư viện này.

### Bảng Tra Cứu Nhanh (Quick Reference)

| Tính năng | Class / Annotation | Mục đích |
|---|---|---|
| **Chuẩn hóa Response** | `ApiResponse<T>` | Định dạng chuẩn trả về cho client (`code`, `message`, `data`, `timestamp`). |
| **Phân trang** | `PageResponse<T>` | Object bọc dữ liệu danh sách phân trang (`items`, `page`, `size`, `totalElements`, `totalPages`, `hasNext`, `hasPrevious`). |
| **Bọc Response tự động** | `GlobalResponseBodyAdvice` | Tự động bọc dữ liệu trả về từ Controller thành `ApiResponse`. |
| **Bỏ qua bọc Response** | `@IgnoreResponseAdvice` | Dùng ở Controller/Method để trả về dữ liệu thô (gốc). |
| **Lỗi nghiệp vụ** | `BaseBusinessException` | Lớp cha cho các custom exception có mã lỗi nghiệp vụ. |
| **Chi tiết lỗi Validate** | `FieldErrorDetail` | Trả về chi tiết các trường bị lỗi khi dùng `@Valid`. |
| **Mã lỗi** | `ErrorCode` | Enum định nghĩa mã lỗi chuẩn và HTTP Status tương ứng. |
| **Bắt lỗi toàn cục** | `GlobalExceptionHandler` | ControllerAdvice xử lý exception tập trung, trả về `ResponseEntity` đúng HTTP Status. |
| **Validate Enum** | `@ValidEnum` | Annotation kiểm tra giá trị String có thuộc Enum hợp lệ không. |
| **Tracking Request** | `TraceIdFilter` | Filter thêm/kế thừa `X-Correlation-Id` và gán vào MDC log. |
| **Auditing DTO/Entity** | `IAuditable` | Interface chuẩn hóa khai báo trường thời gian (`createdAt`, `updatedAt`). |
| **Tiện ích JSON** | `JsonUtils` | Cung cấp hàm `toJson`, `fromJson` tiện lợi (sử dụng `ObjectMapper` đã chuẩn hóa). |
| **Tiện ích Thời gian** | `TimeUtils` | Xử lý thời gian UTC, Epoch Milliseconds (13 chữ số). |
| **Kiểu thời gian JSON** | `UtcTimestamp` | Object bọc Epoch Milli cho request/response. |

---

## 📖 2. Hướng Dẫn Sử Dụng Cơ Bản (Quick Start)

### 2.1 Chuẩn Hóa Response & Tự Động Bọc Dữ Liệu

Mọi API trả về sẽ tự động được bọc trong class `ApiResponse<T>`.

#### Sử dụng thông thường trong Controller:
Bạn chỉ cần trả về Object, String hoặc Collection như bình thường. Thư viện sẽ tự động bọc lại:

```java
@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable Long id) {
        // Không cần ApiResponse.success(user), chỉ cần return user
        return userService.getUserById(id); 
    }
}
```

#### Kết quả trả về cho Client:
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

#### Bypass (Bỏ qua) tự động bọc Response:
Nếu bạn đang viết API cho đối tác thứ 3, hoặc export file CSV, webhook... hãy dùng `@IgnoreResponseAdvice` để trả về nguyên bản dữ liệu:

```java
@GetMapping("/export")
@IgnoreResponseAdvice // Áp dụng ở method (hoặc ở class Controller)
public String exportCsv() {
    return "id,name
1,John";
}
```

---

### 2.2 Phân Trang Với PageResponse

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

Dữ liệu trả về sẽ được tự động bọc bởi `ApiResponse` (nằm trong trường `data`), cung cấp đầy đủ thông tin phân trang cho client.

---

### 2.3 Quản Lý Exception & Xử Lý Lỗi Tập Trung

#### Sử dụng ErrorCode định nghĩa sẵn:
Thay vì ném `RuntimeException` chung chung, hãy kế thừa `BaseBusinessException`:

```java
public class NotFoundException extends BaseBusinessException {
    public NotFoundException(String message) {
        super(ErrorCode.NOT_FOUND, message);
    }
}
```

#### Ném lỗi trong Service:
```java
@Service
public class UserService {
    public UserDto getUserById(Long id) {
        return userRepository.findById(id)
            .orElseThrow(() -> new NotFoundException("User not found with id: " + id));
    }
}
```

`GlobalExceptionHandler` sẽ tự động bắt `NotFoundException`, log ở mức `WARN` (không in stacktrace để giảm nhiễu log) và trả về Response tương ứng:

```json
{
  "code": 404,
  "message": "User not found with id: 1",
  "data": null,
  "timestamp": 1724916781500
}
```

#### Xử Lý Lỗi Validate (`@Valid`):
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

#### Custom Annotation `@ValidEnum`:
Thư viện cung cấp sẵn annotation `@ValidEnum` để tự động kiểm tra một chuỗi đầu vào (String) có khớp với giá trị của một Enum cho trước hay không (hỗ trợ `ignoreCase`):

```java
public class CreateOrderRequest {
    @ValidEnum(enumClass = OrderStatus.class, ignoreCase = true, message = "Status must be valid")
    private String status; 
}
```

---

## 🔍 3. Tracing Request (TraceId & MDC)

### Luồng Hoạt Động Của TraceIdFilter

`TraceIdFilter` tự động chạy với độ ưu tiên cao nhất (`Ordered.HIGHEST_PRECEDENCE`):
1. Kiểm tra header `X-Correlation-Id`. Nếu gọi chéo service (microservice A gọi microservice B) đã có sẵn header, nó sẽ dùng lại `traceId` đó.
2. Nếu request gọi từ bên ngoài vào chưa có header, nó sẽ tự sinh một `UUID` mới.
3. TraceId này được đưa vào header của response trả về client, đồng thời được nạp vào `MDC` (Mapped Diagnostic Context) với key là `traceId`.

> [!IMPORTANT]
> **Chống rò rỉ TraceID (MDC Cleanup):** Filter sử dụng khối `try-finally` để gọi `MDC.remove("traceId")` khi kết thúc luồng request. Việc này ngăn chặn triệt me tình trạng rò rỉ (leak) TraceID sang các request khác dùng chung Thread trong Thread Pool của Tomcat.

### Cấu Hình Logback (`logback-spring.xml`)

Để log của bạn hiển thị `traceId`, hãy thêm `%X{traceId}` vào log pattern tại `src/main/resources/logback-spring.xml`:

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

## ⚡ 4. Xử Lý Thời Gian & JSON (Utilities)

Thư viện chuẩn hóa việc sử dụng thời gian dạng UTC Epoch Milliseconds (13 chữ số) để tránh lỗi parse Timezone.

### 4.1 Chuẩn Hóa Auditing & TimeUtils

#### Interface `IAuditable`:
Cung cấp một interface chung bắt buộc khai báo `createdAt` và `updatedAt` theo định dạng chuẩn `Long` (13 chữ số Epoch Milli):

```java
public class UserEntity implements IAuditable {
    private Long createdAt;
    private Long updatedAt;
    
    // Implement getters & setters...
}
```

#### Sử dụng `TimeUtils`:
```java
long now = TimeUtils.nowEpochMilli(); // 1724916781123
String iso8601 = TimeUtils.toUtcString(now); // 2024-08-29T07:33:01.123Z
```

---

### 4.2 DTO UtcTimestamp & Jackson Customizer

Sử dụng `UtcTimestamp` trong Request/Response DTO. Khi gọi API, client truyền số 13 chữ số, Jackson sẽ tự động map, và khi log nó sẽ hiển thị dạng đọc được:

```java
public class UserDto {
    private String name;
    private UtcTimestamp createdAt; // Jackson map to/from Long (epoch milli)
}

// Khi in log:
log.info("Created at: {}", dto.getCreatedAt()); 
// Result: Created at: 2024-08-29 14:35:51 UTC (1724916781123)
```

> [!NOTE]
> Thư viện đăng ký `Jackson2ObjectMapperBuilderCustomizer` tự động. Do đó, các cấu hình Jackson (kèm `JavaTimeModule`) được áp dụng nhất quán cho **toàn bộ** `ObjectMapper` do Spring Boot khởi tạo (bao gồm RestTemplate, Kafka, Redis...).

---

### 4.3 Tiện Ích JSON (JsonUtils)

```java
// Convert Object -> JSON String
String json = JsonUtils.toJson(new UserDto());

// Parse JSON String -> Object
UserDto user = JsonUtils.fromJson(json, UserDto.class);

// Parse Generic Type (List, Map...)
List<UserDto> users = JsonUtils.fromJson("[{...}]", new TypeReference<List<UserDto>>() {});
```

---

## ⚠️ 5. Lưu Ý Thực Tế & Góc Khuất (Edge Cases & Gotchas)

Dưới đây là các lưu ý và thiết kế đặc biệt trong `service-kit-common` để tránh lỗi vô lý trong sản phẩm:

| # | Vấn đề / Bẫy thực tế | Tác động | Giải pháp đã xử lý sẵn |
|---|---|---|---|
| **1** | **`String` Response `ClassCastException`** | Trong Spring Web, nếu Controller trả về trực tiếp kiểu `String`, `StringHttpMessageConverter` sẽ chạy trước `MappingJackson2HttpMessageConverter`. Nếu bọc `String` vào `ApiResponse` thô, Spring sẽ ném `ClassCastException`. | `GlobalResponseBodyAdvice` chủ động kiểm tra nếu return type là `String`, nó sẽ tự gọi `ObjectMapper` để serialize thành chuỗi JSON `ApiResponse` hoàn chỉnh trước khi trả về. |
| **2** | **Semantic HTTP Status Code** | Trả về HTTP 200 OK và giấu lỗi bên trong response body (anti-pattern) làm API Gateway, Prometheus, Load Balancer bị "mù" lỗi hệ thống. | `GlobalExceptionHandler` trả về đúng chuẩn `ResponseEntity` với HTTP Status Code chuẩn RFC (400 Bad Request, 404 Not Found, 500 Server Error...) tương ứng với `ErrorCode`. |
| **3** | **Thread Pool MDC Leak** | Khi Tomcat tái sử dụng Thread cho request mới, nếu không xoá `traceId` cũ trong MDC, log của request mới sẽ mang `traceId` của request cũ. | `TraceIdFilter` được bao bọc trong khối `try-finally`, luôn luôn thực thi `MDC.remove("traceId")` ngay khi luồng HTTP request kết thúc. |
| **4** | **Optional Dependency Tránh Nặng Worker** | Nếu service không có Controller (như Kafka Consumer, Cron Job Worker), việc import thư viện common không nên kéo theo Tomcat Server hay Spring Web. | Đánh dấu `<optional>true</optional>` cho `spring-boot-starter-web` và `validation`. Service dạng Worker không lo bị thừa dependency Web. |
| **5** | **Bypassing Auto Wrap API** | Một số API cần trả về file binary (PDF, Excel) hoặc webhook thô từ bên thứ 3. Nếu bị tự động bọc thành JSON `ApiResponse`, API sẽ bị hỏng format. | Sử dụng annotation `@IgnoreResponseAdvice` tại class Controller hoặc method để bỏ qua cơ chế bọc tự động. |

---

## 🧪 6. Hướng Dẫn Viết Unit Test Nhanh

Ví dụ cách test API xem có được bọc `ApiResponse` và có trả về `X-Correlation-Id` đúng không bằng `@WebMvcTest`:

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

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
