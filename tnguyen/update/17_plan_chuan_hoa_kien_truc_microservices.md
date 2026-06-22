# 17 — Plan Chuẩn Hóa Kiến Trúc Microservices

> **Mục tiêu:** Nâng `chat-server-microservices` từ mô hình "Distributed Monolith" lên hệ Microservices đạt chuẩn, khắc phục đủ **6 điểm** đã kiểm chứng + 1 quick-fix DB.
> **Stack hiện tại:** Java 17 · Spring Boot 3.2.4 · Spring Cloud 2023.0.1 · OpenFeign · RabbitMQ · MySQL 8 · Docker Compose.
> **Trạng thái:** 📋 Planned — chưa triển khai.
> **Ngày tạo:** 2026-06-19.

---

## 0. Bối cảnh & kết quả kiểm chứng

6 vấn đề đã được đối chiếu trực tiếp với source code (xác nhận **đều tồn tại thật**):

| # | Vấn đề | Bằng chứng trong code |
|---|---|---|
| 1 | Không có Outbox Pattern (dual-write) | `MessageService.java`: `repository.save()` (L206) tách rời `rabbitTemplate.convertAndSend()` (L112/181/186), không cùng transaction |
| 2 | Giao tiếp nội bộ bằng REST, không gRPC | Toàn bộ `@FeignClient`; `grep grpc/protobuf` trong mọi `pom.xml` = 0 |
| 3 | Không có Circuit Breaker / Retry | `grep resilience4j` = 0; Feign chỉ bọc `try/catch` thủ công |
| 4 | `ddl-auto=update` + `create-drop`, không Flyway | 6 service trong compose dùng `SPRING_JPA_DDL_AUTO=update`; `notification`/`file` mặc định `create-drop` (mất DB khi restart); `grep flyway/liquibase` = 0 |
| 5 | Thiếu Distributed Tracing | Có Prometheus (12 service) nhưng `grep tracing/zipkin/sleuth` = 0; `log-service` ghi file `./logs/chat_log.txt` |
| 6 | Tự code auth, không IdP tập trung | Có `auth-service` + `chat_auth_db`; `grep keycloak/auth0` = 0 |

**Nguyên tắc thực thi:** YAGNI–KISS–DRY · mỗi phase độc lập build/test được · không phá vỡ tính năng hiện có · commit theo conventional commits.

---

## 1. Tổng quan các Phase

| Phase | Hạng mục | Service ảnh hưởng | Rủi ro | Ước lượng | Ưu tiên |
|---|---|---|---|---|---|
| **P0** | Quick-fix DB + dọn rác | `notification`, `file` + compose | Thấp | 0.5 ngày | 🔴 Ngay |
| **P1** | Resilience4j (CB + Retry + Timeout) | `server`, `channel`, `messaging` (caller Feign) | Thấp | 1.5 ngày | 🔴 Cao |
| **P2** | Distributed Tracing (Micrometer + Zipkin) | Tất cả 12 service | Thấp | 1 ngày | 🟠 Cao |
| **P3** | Flyway migration (thay ddl-auto) | 9 service có DB | Trung bình | 2–3 ngày | 🟠 TB |
| **P4** | Outbox Pattern | `messaging` (+ template cho service publish) | Cao | 3 ngày | 🟠 TB |
| **P5** | gRPC cho giao tiếp nội bộ | `role`, `server`, `channel`, `messaging` | Cao | 4–5 ngày | 🟡 Thấp |
| **P6** | Keycloak (External IdP) | `gateway`, `auth`, mọi service verify token, `client-app` | Rất cao | 4–5 ngày | 🟡 Thấp |

> **Thứ tự khuyến nghị:** P0 → P1 → P2 → P3 → P4 → P5 → P6. P0–P2 cho giá trị cao/rủi ro thấp, làm trước. P5–P6 nặng nhất, chỉ làm khi cần chuẩn doanh nghiệp.

---

## 2. Chi tiết từng Phase

### P0 — Quick-fix Database & Dọn dẹp

**Mục tiêu:** Chặn rủi ro mất DB khi restart; dọn rác repo.

**Việc cần làm:**
1. `notification-service/src/main/resources/application.yml` & `application-docker.yml`: đổi `ddl-auto: create-drop`/`update` → `update` (tạm, sẽ thành `validate` ở P3).
2. `file-service/...`: tương tự — bỏ `create-drop`.
3. Thêm `bin/` vào `.gitignore`; xóa 8 thư mục `bin/` đang bị track (rác Eclipse).
4. Bỏ giá trị mặc định JWT secret hardcode trong `gateway`/`auth` `application.yml` → fail-fast nếu thiếu env `JWT_SECRET`.

**Acceptance:** restart `docker compose` không mất dữ liệu `notification`/`file`; `git status` sạch; thiếu `JWT_SECRET` thì app log lỗi rõ ràng.

---

### P1 — Resilience4j (Circuit Breaker + Retry + Timeout)

**Mục tiêu:** Ngăn cascade failure khi một service chết; mọi lời gọi Feign có timeout + ngắt mạch + fallback.

**Service ảnh hưởng:** caller có `@FeignClient` → `server-service`, `channel-service`, `messaging-service`.

**Dependency (thêm vào pom của caller, version do Spring Cloud 2023.0.1 quản):**
```xml
<dependency>
  <groupId>org.springframework.cloud</groupId>
  <artifactId>spring-cloud-starter-circuitbreaker-resilience4j</artifactId>
</dependency>
```

**Bật Feign + CircuitBreaker** (application.yml mỗi caller):
```yaml
spring:
  cloud:
    openfeign:
      circuitbreaker:
        enabled: true
      client:
        config:
          default:
            connectTimeout: 2000
            readTimeout: 3000
resilience4j:
  circuitbreaker:
    instances:
      default:
        slidingWindowSize: 10
        failureRateThreshold: 50
        waitDurationInOpenState: 10s
  retry:
    instances:
      default:
        maxAttempts: 3
        waitDuration: 500ms
```

**Code:** mỗi `@FeignClient` thêm `fallback`/`fallbackFactory`:
- `RoleClient` (3 bản: server, channel, messaging) → `RoleClientFallback` trả default an toàn (vd: deny permission khi role-service down).
- `ChannelClient` (server) → fallback.
- `ServerServiceClient` (messaging) → fallback cho `isServerMember` trả `false` + log cảnh báo.

**Acceptance:** tắt `role-service` → caller không treo thread, trả fallback trong < 3s; actuator `/actuator/health` hiển thị circuit state; viết 1 unit test giả lập Feign timeout cho mỗi caller.

---

### P2 — Distributed Tracing (Micrometer Tracing + Zipkin)

**Mục tiêu:** Trace một request xuyên nhiều service bằng TraceId/SpanId tự động qua Gateway → Feign → RabbitMQ.

**Service ảnh hưởng:** tất cả 12 service (thêm dependency chung — cân nhắc đưa vào `common-lib` để DRY).

**Dependency:**
```xml
<dependency>
  <groupId>io.micrometer</groupId>
  <artifactId>micrometer-tracing-bridge-brave</artifactId>
</dependency>
<dependency>
  <groupId>io.zipkin.reporter2</groupId>
  <artifactId>zipkin-reporter-brave</artifactId>
</dependency>
<!-- để trace lan qua Feign -->
<dependency>
  <groupId>io.github.openfeign</groupId>
  <artifactId>feign-micrometer</artifactId>
</dependency>
```

**Config (application.yml chung):**
```yaml
management:
  tracing:
    sampling:
      probability: 1.0   # demo: trace 100%, production hạ xuống 0.1
  zipkin:
    tracing:
      endpoint: ${ZIPKIN_URL:http://localhost:9411}/api/v2/spans
logging:
  pattern:
    level: "%5p [${spring.application.name:},%X{traceId:-},%X{spanId:-}]"
```

**Docker Compose:** thêm service `zipkin` (`openzipkin/zipkin:latest`, port `9411`) vào `chat-net`; truyền `ZIPKIN_URL` cho 12 service.

**RabbitMQ propagation:** Micrometer tự inject trace context vào header AMQP khi dùng `RabbitTemplate` chuẩn — verify producer/consumer giữ được traceId.

**(Tùy chọn) Centralized logging:** thay vì file `chat_log.txt`, ship log qua Loki + Grafana (đã có Grafana sẵn ở `devops/`). Giữ `log-service` hiện tại làm audit log nghiệp vụ.

**Acceptance:** gửi 1 tin nhắn → mở Zipkin UI thấy 1 trace nối Gateway → messaging → (Feign) server/role → (RabbitMQ) notification/log; traceId xuất hiện trong log mọi service.

---

### P3 — Flyway Migration (thay Hibernate ddl-auto)

**Mục tiêu:** Quản lý schema versioned, bỏ `ddl-auto=update`, chuyển sang `validate`.

**Service ảnh hưởng:** 9 service có DB (`auth`, `server`, `channel`, `messaging`, `presence`, `notification`, `file`, `user-profile`, `role`, `friend`).

**Dependency:**
```xml
<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-core</artifactId></dependency>
<dependency><groupId>org.flywaydb</groupId><artifactId>flyway-mysql</artifactId></dependency>
```

**Quy trình mỗi service:**
1. Dump schema hiện tại (sinh bởi Hibernate) → viết `V1__init_schema.sql` đặt tại `src/main/resources/db/migration/`.
2. Đổi `application.yml`:
   ```yaml
   spring:
     jpa:
       hibernate:
         ddl-auto: validate
     flyway:
       enabled: true
       baseline-on-migrate: true
   ```
3. Bỏ `SPRING_JPA_DDL_AUTO=update` khỏi `docker-compose.yml` (hoặc set `validate`).

**Acceptance:** mỗi service khởi động chạy Flyway tạo bảng `flyway_schema_history`; `ddl-auto: validate` không báo mismatch; restart không đổi schema ngoài ý muốn.

**Lưu ý:** làm tuần tự từng service, test kỹ — đây là phase dễ vỡ dữ liệu nếu schema dump sai.

---

### P4 — Outbox Pattern (đảm bảo nhất quán dual-write)

**Mục tiêu:** Loại bỏ rủi ro mất sự kiện khi save DB OK nhưng publish RabbitMQ fail (và ngược lại).

**Service ảnh hưởng:** ưu tiên `messaging-service` (publish nhiều nhất); tạo template tái dùng cho service khác.

**Thiết kế (Polling Publisher — KISS, không cần Debezium):**
1. Bảng `outbox_event` (mỗi service publish): `id, aggregate_type, aggregate_id, event_type, payload(JSON), exchange, routing_key, status(PENDING/SENT), created_at, sent_at`. Thêm vào Flyway `V2__outbox.sql`.
2. Trong cùng `@Transactional` với `messageRepository.save(entity)`: ghi luôn 1 dòng `outbox_event` (thay vì gọi `convertAndSend` trực tiếp).
3. `OutboxPublisher` (`@Scheduled` mỗi 1s, hoặc `@Scheduled` + `SELECT ... FOR UPDATE SKIP LOCKED`): quét `PENDING` → `rabbitTemplate.convertAndSend(...)` → update `SENT`.
4. Consumer phải **idempotent** (dedup theo `eventId`) vì at-least-once.

**Refactor `MessageService`:** thay 3 lời gọi `convertAndSend` (L112/181/186) bằng ghi outbox trong transaction.

**Acceptance:** kill RabbitMQ giữa lúc save → message lưu vào outbox PENDING; RabbitMQ sống lại → publisher tự đẩy đi; không mất sự kiện; consumer không xử lý trùng.

---

### P5 — gRPC cho giao tiếp đồng bộ nội bộ

**Mục tiêu:** Thay REST/Feign nội bộ bằng gRPC (binary, hiệu năng cao). **Cân nhắc:** nặng, chỉ làm nếu yêu cầu chuẩn doanh nghiệp — Feign vẫn hợp lệ.

**Service ảnh hưởng:** cặp gọi nhau nhiều: `role` (server), `server`/`channel`/`messaging` (caller).

**Dependency & build:**
```xml
<dependency><groupId>net.devh</groupId><artifactId>grpc-spring-boot-starter</artifactId><version>3.1.0.RELEASE</version></dependency>
<!-- + protobuf-maven-plugin + os-maven-plugin để compile .proto -->
```

**Việc cần làm:**
1. Định nghĩa `.proto` cho contract: `RoleService` (checkPermission, getRoles), `ServerService` (getMembers). Đặt trong `common-lib/src/main/proto/`.
2. Service callee: implement `@GrpcService`.
3. Caller: dùng `@GrpcClient` stub thay `@FeignClient`.
4. Giữ REST endpoint qua Gateway cho client bên ngoài; gRPC chỉ cho internal.
5. Compose: mở cổng gRPC nội bộ (vd 9090) trên `chat-net`, không publish ra ngoài.

**Acceptance:** `messaging` gọi `role` qua gRPC thành công; benchmark latency thấp hơn REST; REST cũ qua Gateway vẫn chạy.

**Rủi ro:** trùng lặp contract REST+gRPC, tăng độ phức tạp build (protoc). Đánh giá ROI trước khi làm.

---

### P6 — Keycloak (External Identity Provider)

**Mục tiêu:** Chuyển xác thực sang IdP chuyên nghiệp; Gateway chỉ verify token (OAuth2 Resource Server). **Cân nhắc:** rất nặng, làm mất phần lõi học tập của đồ án Java — chỉ làm nếu bắt buộc.

**Service ảnh hưởng:** `gateway` (verify), `auth-service` (thu hẹp vai trò/loại bỏ), mọi service downstream, `client-app` (login flow OIDC).

**Việc cần làm:**
1. Compose: thêm `keycloak` (`quay.io/keycloak/keycloak`) + DB riêng; tạo realm `chatserver`, client `chat-client`.
2. Gateway: thay `JwtAuthFilter` thủ công bằng `spring-boot-starter-oauth2-resource-server` (validate JWT bằng JWKS của Keycloak).
3. Downstream: nhận claim từ token Keycloak (sub, roles) thay `X-User-Id` tự inject.
4. `client-app` (Swing): triển khai OIDC login (Authorization Code / Password grant) lấy token từ Keycloak.
5. Migrate user hiện có từ `chat_auth_db` → Keycloak (script import) hoặc giữ `auth-service` làm user federation.

**Acceptance:** login qua Keycloak nhận JWT hợp lệ; Gateway verify bằng JWKS; RBAC map từ Keycloak roles; SSO hoạt động.

**Rủi ro cao:** thay đổi luồng auth toàn hệ thống + client. Cân nhắc giữ `auth-service` song song trong giai đoạn chuyển tiếp.

---

## 3. Dependency tổng hợp cần thêm

| Phase | Artifact | Phạm vi |
|---|---|---|
| P1 | `spring-cloud-starter-circuitbreaker-resilience4j` | caller Feign |
| P2 | `micrometer-tracing-bridge-brave`, `zipkin-reporter-brave`, `feign-micrometer` | tất cả (qua common-lib) |
| P3 | `flyway-core`, `flyway-mysql` | 9 service DB |
| P5 | `grpc-spring-boot-starter` + `protobuf-maven-plugin` | role/server/channel/messaging + common-lib |
| P6 | `spring-boot-starter-oauth2-resource-server` | gateway (+ downstream) |

Hạ tầng compose thêm: **Zipkin** (P2), **Keycloak + DB** (P6).

---

## 4. Chiến lược test & rollout

- Mỗi phase: 1 nhánh git riêng (`feat/p1-resilience4j`, …), PR riêng, build `./mvnw clean package` xanh trước khi merge.
- Bổ sung test cho service lõi đang thiếu (`auth`, `server`, `channel`, `gateway`, `role`, `friend`, `presence`, `user-profile`) — hiện chỉ 9 file test toàn repo.
- Smoke test sau mỗi phase: `docker compose up` → gửi tin nhắn end-to-end qua `client-app`.
- Không bao giờ merge khi test fail (tuân thủ pre-push rule).

---

## 5. Câu hỏi chưa giải quyết

1. **gRPC (P5) & Keycloak (P6)** có thực sự cần cho đồ án SE330 không, hay over-engineering? → cần xác nhận yêu cầu giảng viên trước khi tốn 8–10 ngày công.
2. **Centralized logging**: dùng Loki (nhẹ, hợp Grafana sẵn có) hay ELK (nặng hơn)? Mặc định đề xuất Loki.
3. **Outbox (P4)** áp cho mọi service publish hay chỉ `messaging`? Đề xuất làm `messaging` trước, nhân rộng sau.
4. File spec gốc `Global_Microservices_Architecture_Specification.md` **không có trong repo** — plan này bám theo best practice ngành, cần đối chiếu lại nếu tìm thấy spec.
