# Master Plan: Refactor Hệ thống Microservices Toàn Diện

Bản kế hoạch toàn diện (Master Plan) này nhằm hoạch định lộ trình chuyển đổi dự án `chat-server-microservices` từ trạng thái "Distributed Monolith" (Khối nguyên khối phân tán) trở thành một hệ thống Microservices đạt chuẩn công nghiệp, tuân thủ nghiêm ngặt các nguyên lý của Domain-Driven Design (DDD) và Cloud-Native.

## User Review Required

> [!WARNING]
> Kế hoạch này phác thảo lộ trình đường dài cho toàn bộ dự án. Vui lòng xem xét sự đánh đổi (trade-offs) về mặt thời gian và nguồn lực cho từng Phase. Để thực hiện thành công, đội ngũ cần kiên nhẫn tuân thủ thứ tự các Phase thay vì nhảy cóc.

## Open Questions

> [!IMPORTANT]
> 1. Thời gian dự kiến (Deadline) cho từng Phase là bao lâu? (Gợi ý: Phase 1: 3-4 tuần, Phase 2: 4-6 tuần, Phase 3: 2-3 tuần).
> 2. Đội ngũ có sẵn sàng áp dụng Kubernetes ở Phase 3 hay vẫn muốn giữ ở mức Docker Compose / Docker Swarm để tiết kiệm chi phí/tài nguyên?

---

## 🎯 PHASE 1: Macro-Architecture & Nền Tảng Hạ Tầng (Infrastructure Stabilization)
**Mục tiêu:** Xây dựng hệ thống lưới bảo vệ (safety net), đảm bảo tính toàn vẹn dữ liệu, khả năng chịu lỗi mạng và giám sát toàn diện trước khi đụng vào logic nghiệp vụ bên trong.

### 1.1 Quản trị Dữ liệu & Identity (Data & Identity Management)
- **Tích hợp Flyway/Liquibase:** Xóa bỏ hoàn toàn `SPRING_JPA_DDL_AUTO=update`. Quản lý cấu trúc schema database qua versioning (V1, V2...) tự động chạy khi khởi động service.
- **Tái cấu trúc Identity Provider:** Loại bỏ `auth-service` tự xây dựng cục bộ. Tích hợp giải pháp chuyên dụng (Keycloak/Auth0) làm Centralized IdP. API Gateway chịu trách nhiệm xác thực token (Authentication).

### 1.2 Giám Sát Phân Tán (Observability Triad)
- **Distributed Tracing:** Tích hợp `micrometer-tracing-bridge-otel`. Gắn `TraceId` và `SpanId` cho mọi request HTTP, gRPC và RabbitMQ message lan truyền qua các service.
- **Centralized Logging:** Triển khai hạ tầng gom log tập trung ELK Stack (Elasticsearch, Logstash, Kibana) hoặc Grafana Loki.
- **Structured Logging:** Định dạng output log của toàn bộ service sang định dạng JSON để dễ dàng index và tìm kiếm.

### 1.3 Tích Hợp & Chịu Lỗi (Integration & Resilience)
- **Pattern Kháng Lỗi:** Tích hợp thư viện `resilience4j`. Áp dụng Circuit Breaker, Retry, Timeout cho mọi cuộc gọi mạng đồng bộ.
- **Chuẩn Hóa Lỗi (Problem Details):** Tạo `GlobalExceptionHandler` chung để bắt lỗi và trả về chuẩn RFC 7807, che giấu hoàn toàn Stack Trace.
- **Khởi tạo gRPC:** Xây dựng module `grpc-contracts`, bắt đầu chuyển đổi các REST API nội bộ có traffic cao sang gRPC.
- **Strict Decoupling (Dọn dẹp Common-lib):** Gỡ bỏ toàn bộ Business DTOs ra khỏi `common-lib`. `common-lib` chỉ giữ các file cấu hình kỹ thuật (Exceptions, JWT parser, Utils). Các service giao tiếp qua Anti-Corruption Layer hoặc chung file `.proto`.

### 1.4 Hướng Sự Kiện & Nhất Quán Dữ Liệu (Event-Driven & Consistency)
- **Transactional Outbox Pattern:** Giải quyết triệt để bài toán Dual-Write. Thiết kế bảng Outbox, lưu sự kiện vào DB cùng transaction với logic nghiệp vụ.
- **Background Message Relay:** Xây dựng Background Worker (Scheduler) định kỳ quét bảng Outbox để publish tin nhắn lên RabbitMQ an toàn (chuẩn At-least-once delivery).

---

## 🎯 PHASE 2: Micro-Architecture & Domain-Driven Design (Internal Restructuring)
**Mục tiêu:** Mở từng service ra, đập bỏ và tái cấu trúc sâu bên trong mã nguồn để đảm bảo khả năng bảo trì, khả năng mở rộng (Maintainability & Scalability).

### 2.1 Tái cấu trúc mã nguồn (Codebase Architecture)
- **Clean/Vertical Slice Architecture:** Phá bỏ mô hình Layered N-Tier (Controller-Service-Repository) kiểu cũ. Thiết kế lại mã nguồn chia theo cụm tính năng (Feature-based) để khi sửa đổi một tính năng không ảnh hưởng đến tính năng khác.
- **Tách biệt Core Domain:** Đảm bảo tầng nghiệp vụ lõi (Domain Layer) hoàn toàn "sạch", không phụ thuộc vào bất kỳ framework kỹ thuật nào (Spring, Hibernate, RabbitMQ). Mọi giao tiếp với Data Access phải thông qua Dependency Inversion (Interface).

### 2.2 Làm giàu Domain Model (Rich Domain Modeling)
- **Loại bỏ Anemic Domain Model:** Chuyển đổi các Entity từ trạng thái "túi chứa dữ liệu rỗng" (chỉ có các property và getter/setter) sang các thực thể thông minh chứa trọn logic nghiệp vụ bên trong nó.
- **Đóng gói trạng thái (State Encapsulation):** Mọi sự thay đổi trạng thái (state) của Entity bắt buộc phải thông qua các method có ý nghĩa nghiệp vụ thay vì update property trực tiếp (ví dụ: `order.confirmPayment()` thay vì `order.setStatus("PAID")`).
- **Sử dụng Value Objects:** Đóng gói các kiểu dữ liệu nguyên thủy vào Value Objects có khả năng tự validate (ví dụ: class `Email`, `Address`, `Money`) thay vì dùng `String`, `int` rải rác.

### 2.3 Phân quyền phi tập trung & Hoàn thiện giao tiếp (Decentralized Auth & Comm)
- **Decentralized Authorization (RBAC/CBAC):** Các service nội bộ tự phân tích JWT Claims (đã được verify từ Gateway) để thực thi kiểm tra phân quyền tại chỗ thay vì phải gọi mạng ngược lại IdP.
- **100% gRPC nội bộ:** Xóa sổ triệt để mọi đoạn code dùng REST client (Feign/WebClient/RestTemplate) dùng cho giao tiếp Service-to-Service.

---

## 🎯 PHASE 3: Production Readiness & Cloud-Native (Khả Năng Chịu Tải & Vận Hành)
**Mục tiêu:** Đưa hệ thống lên môi trường thực tế (Production), tối ưu hóa để chịu tải khổng lồ và tự động hóa quy trình vận hành.

### 3.1 Vận hành Container & Điều phối (Container Orchestration)
- **Kubernetes (K8s) Migration:** Rời bỏ `docker-compose`. Đưa hệ thống lên Kubernetes thông qua việc viết Helm Charts hoặc manifest YAML (Deployments, Services, Ingress).
- **Cấu hình tập trung (Externalized Configuration):** Lưu trữ tập trung các thông tin nhạy cảm và biến môi trường vào ConfigMap và Kubernetes Secrets (hoặc HashiCorp Vault).
- **Auto-scaling:** Cấu hình Horizontal Pod Autoscaler (HPA), tự động nhân bản (scale out) các service khi mức tiêu thụ CPU/RAM vượt ngưỡng an toàn.

### 3.2 Tối ưu Hiệu năng & Bảo mật (Performance & Security Hardening)
- **Multi-stage Docker Builds:** Chuyển sang sử dụng các base image tối giản nhất (Alpine hoặc Distroless). Thiết lập để ứng dụng chạy dưới quyền `non-root user` để ngăn chặn rủi ro leo thang đặc quyền.
- **Distributed Caching:** Tích hợp Redis cho các hành động đọc nhiều (Read-heavy operations) như Load User Profile, Session State, nhằm giảm tải cho Database.

### 3.3 Tự động hóa CI/CD (Continuous Integration / Continuous Deployment)
- **Automated Testing Pipeline:** CI pipeline bắt buộc chạy Unit Test và Integration Test với tỷ lệ Code Coverage (Jacoco) tối thiểu đạt 80%.
- **Zero-Downtime Deployment:** Cấu hình chiến lược triển khai cập nhật phiên bản mới (Rolling Update, Blue/Green hoặc Canary Deployment) để bảo đảm user không gặp lỗi HTTP 502 trong lúc deploy code.
- **Automated DB Migrations:** Đưa lệnh chạy Flyway/Liquibase vào CI/CD pipeline để cập nhật DB một cách an toàn trước khi container code mới được khởi động.

---

## Verification Plan (Kế hoạch Kiểm Thử Tổng Thể)

### Automated Tests
- **Unit/Integration Tests:** Kiểm thử độc lập phần Domain Logic (Phase 2) không cần Spring Context.
- **Chaos Testing:** Tắt ngẫu nhiên các Pod/Container (dùng công cụ Chaos Monkey) để chứng minh khả năng phục hồi tự động của Kubernetes và sức chịu đựng của Circuit Breaker.
- **Load Testing:** Dùng công cụ JMeter/k6 tạo ra 10.000 concurrent requests (truy cập đồng thời) để kiểm tra HPA (Autoscaling) và xem log có bị nghẽn không.

### Manual Verification
- Thực hiện cập nhật tính năng mới: Gõ lệnh Push code và quan sát Pipeline CI/CD tự động test, tự động build image, và triển khai version mới lên Kubernetes mà trình duyệt người dùng không hề bị đứt đoạn hay cần tải lại trang.
