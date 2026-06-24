# Bảng Phân Chia Công Việc 4 Thành Viên (Phase 1, 2, 3)

Dựa trên Master Plan đã được duyệt, dưới đây là danh sách công việc (Task Checklist) chi tiết chia cho 4 thành viên để bám sát và thực thi xuyên suốt vòng đời dự án. 

## 🧑‍💻 Member 1: Kỹ Sư Dữ Liệu & Hạ Tầng (Data & Infra Engineer)
**Trách nhiệm:** Quản trị Schema Database, Hệ thống Định danh (Identity) và Môi trường chạy thực tế (Kubernetes).

- [ ] **Phase 1: Ổn định Dữ Liệu & Xác Thực**
  - [ ] Loại bỏ hoàn toàn `SPRING_JPA_DDL_AUTO=update` ở mọi file cấu hình.
  - [ ] Tích hợp Flyway hoặc Liquibase; xuất và viết script `V1__init_schema.sql` cho từng database.
  - [ ] Tích hợp Keycloak vào file `docker-compose.yml`. Xóa bỏ code `auth-service` tự chế. Cấu hình API Gateway xác thực JWT.
- [ ] **Phase 2: Phân Quyền Phi Tập Trung**
  - [ ] Viết cấu hình Spring Security SecurityContext cho các service nội bộ để tự parse JWT Claims và phân quyền (RBAC/CBAC) mà không cần gọi lại Keycloak.
- [ ] **Phase 3: Kubernetes Migration & Hardening**
  - [ ] Viết Helm Charts hoặc YAML Manifests để chuyển hạ tầng từ Docker Compose sang Kubernetes (K8s).
  - [ ] Đưa toàn bộ cấu hình, password nhạy cảm vào Kubernetes ConfigMap & Secrets.
  - [ ] Tối ưu hóa Multi-stage Docker Builds, chuyển sang image Alpine/Distroless và chạy dưới quyền `non-root user`.
  - [ ] Cấu hình Horizontal Pod Autoscaler (HPA) tự động scale các Pod.

## 🧑‍💻 Member 2: Kỹ Sư Giám Sát & Hiệu Năng (Observability & Performance)
**Trách nhiệm:** Xây dựng hệ thống Tracing, Centralized Logging, Caching và chịu trách nhiệm Load Testing.

- [ ] **Phase 1: Giám Sát Phân Tán (Observability)**
  - [ ] Tích hợp thư viện `micrometer-tracing-bridge-otel` để sinh và truyền tải TraceId/SpanId.
  - [ ] Dựng hạ tầng ELK Stack (Elasticsearch, Logstash, Kibana) hoặc Grafana Loki trên môi trường local.
  - [ ] Cấu hình Structured JSON Logging (thông qua `logback-spring.xml`) bắt TraceId ở mọi dòng log của mọi service.
- [ ] **Phase 2: Mở Rộng Giám Sát Nghiệp Vụ**
  - [ ] Định nghĩa thêm các custom metrics (ví dụ: số lượng tin nhắn gửi thành công/s, lượng user online) để đẩy về hệ thống Prometheus Dashboard.
- [ ] **Phase 3: Bộ Nhớ Đệm & Chịu Tải Kịch Trần**
  - [ ] Tích hợp Redis làm Distributed Caching cho các logic lấy User Profile, Role Check (Read-heavy operations).
  - [ ] Chạy Chaos Testing: Dùng kịch bản ngẫu nhiên tắt nóng (kill) container DB/RabbitMQ để kiểm tra sức phục hồi của hệ thống.
  - [ ] Chạy Load Testing bằng JMeter/k6 với 10,000 CCU đồng thời vào Gateway.

## 🧑‍💻 Member 3: Kỹ Sư Tích Hợp & Chịu Lỗi (Integration & Resilience)
**Trách nhiệm:** Áp dụng Circuit Breaker, gRPC nội bộ, chuẩn hóa Problem Details và xây dựng đường ống CI/CD.

- [ ] **Phase 1: Mạng Lưới Giao Tiếp & Kháng Lỗi**
  - [ ] Thêm thư viện `resilience4j` và bọc toàn bộ các lời gọi REST client bằng `@CircuitBreaker`, `@Retry`, `@TimeLimiter`.
  - [ ] Thiết kế `GlobalExceptionHandler` trong `common-lib` để ép định dạng lỗi trả về tuân thủ chuẩn RFC 7807, giấu kín Stack Trace.
  - [ ] Setup `grpc-spring-boot-starter` và tạo module `grpc-contracts` dùng chung.
  - [ ] **Dọn dẹp Common-lib (Decoupling):** Gỡ bỏ toàn bộ Business DTOs ra khỏi `common-lib` để dứt điểm tình trạng phụ thuộc chéo.
- [ ] **Phase 2: Chuyển Đổi 100% sang gRPC**
  - [ ] Cày ải xóa sổ toàn bộ REST API dùng cho giao tiếp Service-to-Service. Định nghĩa Protobuf và thay thế 100% bằng gRPC stub.
- [ ] **Phase 3: Tự Động Hóa Triển Khai (CI/CD Pipeline)**
  - [ ] Thiết lập CI Pipeline (Kiểm tra chất lượng code, chạy Flyway Migration trước khi deploy).
  - [ ] Thiết lập Zero-Downtime Deployment (Rolling Update hoặc Blue/Green) trên cụm K8s.

## 🧑‍💻 Member 4: Kỹ Sư Logic Lõi & Sự Kiện (Core Domain & Event-Driven)
**Trách nhiệm:** Xử lý bất đồng bộ, đập đi xây lại cấu trúc mã nguồn nội bộ (Clean Architecture) và Rich Domain Model.

- [ ] **Phase 1: Đảm Bảo Nhất Quán Dữ Liệu Phân Tán**
  - [ ] Thiết kế bảng `OutboxMessage` và cấu hình Transaction lưu dữ liệu nghiệp vụ kèm message vào chung 1 commit.
  - [ ] Viết Background Worker (Cronjob/ShedLock) để định kỳ quét bảng Outbox và publish an toàn lên RabbitMQ.
- [ ] **Phase 2: Tái Cấu Trúc Kiến Trúc Vi Mô (Nhiệm vụ đinh của dự án)**
  - [ ] Loại bỏ cấu trúc N-Tier cũ rích. Bố trí lại mã nguồn từng service theo Vertical Slice Architecture (Feature-based).
  - [ ] Cứu rỗi Anemic Domain Model: Di chuyển logic tính toán, kiểm tra điều kiện từ tầng Service nhồi thẳng vào các class Entity (Rich Domain Model). Đóng gói state chặt chẽ.
  - [ ] Thiết kế lại các kiểu dữ liệu nguyên thủy rải rác thành các Value Objects (`Email`, `Money`).
- [ ] **Phase 3: Tự Động Hóa Kiểm Thử Logic Lõi**
  - [ ] Viết Unit Test và Integration Test độc lập, tập trung sâu vào kiểm thử các Domain Entity và Value Objects vừa refactor. Đảm bảo Test Coverage vượt mốc tối thiểu 80%.
