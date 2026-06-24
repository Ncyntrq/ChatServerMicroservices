# Đánh Giá Hệ Thống Theo Tiêu Chuẩn Microservices

> Phạm vi: toàn bộ `chat-server-microservices` sau khi merge `origin/main` vào `tnguyen/member1`.
> Ngày: 2026-06-24. Cơ sở đánh giá: 12-Factor App + các pattern microservices phổ biến.

## 1. Tổng Quan Kiến Trúc

- **13 service nghiệp vụ** tách theo bounded context: `auth`, `user-profile`, `server`, `channel`, `messaging`, `presence`, `notification`, `file`, `role`, `friend`, `log`, + `gateway` (API Gateway), `client-app` (desktop).
- **Module dùng chung:** `common-lib` (DTO/util), `grpc-contracts` (định nghĩa `.proto`).
- **Giao tiếp:** đồng bộ qua **gRPC** (auth/channel/presence/role/server/user) + bất đồng bộ qua **RabbitMQ** (events, notification, log).
- **Hạ tầng:** MySQL (DB-per-service), MinIO (object storage), RabbitMQ, Keycloak (IdP), Jaeger/Prometheus/Grafana/Loki (observability).

## 2. Bảng Điểm Tiêu Chuẩn

| # | Tiêu chí Microservices | Mức | Ghi chú |
|---|---|---|---|
| 1 | Phân rã theo bounded context | ✅ Tốt | 11 service nghiệp vụ độc lập |
| 2 | Database per Service | ✅ Tốt | 8 DB riêng (`chat_*_db`) + MinIO + presence in-memory |
| 3 | API Gateway | ✅ Tốt | Spring Cloud Gateway, định tuyến tập trung |
| 4 | Xác thực/Phân quyền | ✅ Tốt | Keycloak OIDC + dual-mode JWT (RS256/HMAC) tại gateway |
| 5 | Giao tiếp đồng bộ | ✅ Tốt | gRPC + `grpc-contracts` (hợp đồng rõ ràng) |
| 6 | Giao tiếp bất đồng bộ / Event | ✅ Tốt | RabbitMQ + **Outbox pattern** (messaging) đảm bảo publish tin cậy |
| 7 | Resilience (circuit breaker) | ✅ Tốt | Resilience4j trên các gRPC adapter |
| 8 | Observability (3 trụ cột) | ✅ Tốt | Tracing (Micrometer→OTLP→Jaeger), Metrics (Prometheus/Grafana), Log JSON (Logback ×12 + Loki) |
| 9 | Health Check | ✅ Tốt | Actuator + healthcheck Docker cho mọi service |
| 10 | Schema migration | ✅ Tốt | Flyway (`ddl-auto: validate`, schema do Flyway quản lý) |
| 11 | Containerization | ✅ Tốt | Dockerfile multi-stage, Alpine, non-root, `exec` PID 1 |
| 12 | Orchestration | ✅ Tốt | K8s manifests (base + overlays, HPA, Ingress) |
| 13 | CI/CD | ✅ Tốt | GitHub Actions + Jenkinsfile + SonarQube |
| 14 | Config phi tập trung | 🟡 Khá | Env vars + Spring profiles; chưa có Config Server |
| 15 | Service Discovery | 🟡 Khá | Dựa vào DNS Docker/K8s tĩnh; không có Eureka/Consul |
| 16 | Quản lý secret | 🟡 Khá | `.env`/env vars; chưa có Vault/Secret Manager |
| 17 | Rate limiting tại Gateway | ❌ Thiếu | Chưa có (rủi ro abuse/DoS) |
| 18 | API Versioning | ❌ Thiếu | Route `/api/...` không có `/v1` |
| 19 | Zero-trust downstream (RBAC) | ❌ Thiếu | Downstream tin header `X-User-Id` từ gateway (Part D hoãn) |
| 20 | Test coverage | ❌ Yếu | Chỉ ~8 file test toàn hệ thống |

**Tổng kết:** 13 ✅ / 3 🟡 / 4 ❌ → **Mức trưởng thành: KHÁ–TỐT.** Nền tảng microservices vững (decompose, gRPC, observability, resilience, CI/CD, K8s); thiếu sót chủ yếu ở vận hành nâng cao và chất lượng (rate limit, versioning, zero-trust, test).

## 3. Điểm Mạnh Nổi Bật

1. **Tách dịch vụ + DB-per-service rõ ràng** — đúng nguyên tắc autonomy, không share schema.
2. **gRPC + hợp đồng `.proto`** — giao tiếp nội bộ type-safe, hiệu năng cao, thay thế REST/Feign cũ.
3. **Observability đầy đủ 3 trụ cột** — trace phân tán (Jaeger), metrics (Prometheus/Grafana), log JSON tập trung (Loki). Đây là điểm mạnh hiếm thấy ở đồ án.
4. **Reliability patterns** — Outbox + ShedLock (messaging), Circuit Breaker (Resilience4j), healthcheck + `restart: always`.
5. **Bảo mật cửa ngõ** — Keycloak OIDC, dual-mode cho phép migrate dần; realm đã siết (sslRequired, redirect allowlist).
6. **Hạ tầng triển khai chuyên nghiệp** — Docker non-root, K8s overlays + HPA + Ingress, CI/CD + SonarQube.

## 4. Hạn Chế & Khuyến Nghị (ưu tiên giảm dần)

| Ưu tiên | Hạn chế | Khuyến nghị |
|---|---|---|
| 🔴 Cao | **RBAC downstream chưa zero-trust** | Hoàn thiện Part D: `InternalAuthFilter` + `SecurityFilterChain` trong common-lib để mỗi service tự verify token/header, không mù quáng tin gateway |
| 🔴 Cao | **Rate limiting thiếu** | Bật `RequestRateLimiter` (Redis) tại gateway, đặc biệt cho `/api/auth/**` |
| 🟠 TB | **Test coverage yếu (~8 file)** | Bổ sung unit/integration test (Testcontainers cho DB/RabbitMQ); đưa ngưỡng coverage vào SonarQube quality gate |
| 🟠 TB | **Quản lý secret** | Chuyển secret (DB password, KC secret, JWT) sang K8s Secret/Vault; bỏ default trong `.env.example` |
| 🟡 Thấp | **API versioning** | Thêm tiền tố `/api/v1/` để hỗ trợ tiến hóa hợp đồng |
| 🟡 Thấp | **Config tập trung** | Cân nhắc Spring Cloud Config / K8s ConfigMap khi số biến tăng |
| 🟡 Thấp | **Service discovery tĩnh** | Chấp nhận được với K8s Service DNS; nếu chạy ngoài K8s nên thêm Consul/Eureka |
| ⚪ Dọn | **Rác sau merge** | Thư mục `scratch/` chứa `fix_*.ps1` — nên xóa khỏi repo |

## 5. Lưu Ý Kỹ Thuật Phát Sinh Khi Merge

- `origin/main` có **bug sẵn**: dependency `logstash-logback-encoder` thiếu thẻ đóng `</dependency>` ở 9 `pom.xml` → đã sửa khi giải quyết conflict để build pass.
- `docker-compose.yml` dùng network `chat-net` ở chế độ `external: true` → phải tạo trước: `docker network create chat-net` (hoặc do `docker-compose.devops.yml` tạo).

## 6. Câu Hỏi Còn Mở

1. Có kế hoạch hoàn thiện **Part D (zero-trust RBAC)** không, hay chấp nhận mô hình tin-gateway hiện tại?
2. Mục tiêu coverage test bao nhiêu % cho quality gate SonarQube?
3. `auth-service` (HMAC) giữ song song đến bao giờ trước khi chuyển hẳn sang Keycloak?
