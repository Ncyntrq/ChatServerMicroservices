# KHUÔN MẪU KIẾN TRÚC VÀ ĐẶC TẢ YÊU CẦU TOÀN DIỆN CHO DỰ ÁN MICROSERVICES (AGNOSTIC COMPLIANCE)

Bộ tài liệu này thiết lập các tiêu chuẩn thiết kế, giao tiếp, quản lý dữ liệu, an toàn và vận hành bắt buộc đối với một hệ thống phần mềm phân tán áp dụng kiến trúc Microservices. Bộ đặc tả này mang tính chất **độc lập công nghệ (Technology-Agnostic)**, có thể áp dụng đồng nhất cho mọi ngôn ngữ lập trình và công cụ.

---

## 1. NGUYÊN LÝ PHÂN RÃ VÀ THIẾT KẾ ĐỘC LẬP (BOUNDED CONTEXTS)

Một hệ thống Microservices tiêu chuẩn bắt buộc phải tuân thủ nguyên lý thiết kế hướng tên miền (Domain-Driven Design - DDD) để bóc tách một hệ thống lớn (Monolith) thành các dịch vụ độc lập có ranh giới rõ ràng.

### 1.1 Tính Độc Lập Tuyệt Đối Của Dịch Vụ (Loose Coupling)
* **Độc lập Mã nguồn (Codebase Isolation):** Mỗi Microservice là một kho lưu trữ mã nguồn riêng biệt (Repository độc lập hoặc cấu trúc Monorepo phân rã rõ ràng). Không có sự chia sẻ mã nguồn trực tiếp (Shared Code) chứa logic nghiệp vụ giữa các dịch vụ.
* **Độc lập Triển khai (Independent Deployability):** Mỗi dịch vụ phải có chu kỳ phát triển, kiểm thử và CI/CD riêng. Việc cập nhật hoặc deploy một dịch vụ không được gây ảnh hưởng hay bắt buộc các dịch vụ khác phải deploy lại.

### 1.2 Chiến Lược Cô Lập Dữ Liệu (Database-per-Service)
* **Nghiêm cấm Shared Database Antipattern:** Các dịch vụ tuyệt đối không được truy cập trực tiếp (Đọc/Ghi) vào cơ sở dữ liệu của dịch vụ khác. Mọi hành vi lấy dữ liệu chéo đều phải thông qua giao tiếp API công khai hoặc cơ chế hướng sự kiện (Event-Driven).
* **Đa dạng hóa lưu trữ (Polyglot Persistence):** Lựa chọn công nghệ Database dựa trên đặc thù dữ liệu của dịch vụ:
    * *Dữ liệu Giao dịch phức tạp:* Sử dụng Relational DB (PostgreSQL, MySQL, SQL Server).
    * *Dữ liệu Tốc độ cao/Tạm thời:* Sử dụng Key-Value DB (Redis) hoặc Document DB (MongoDB).
    * *Dữ liệu Tìm kiếm/Phân tích:* Sử dụng Search Engine (Elasticsearch, OpenSearch).

---

## 2. KIẾN TRÚC GIAO TIẾP VÀ TÍCH HỢP HỆ THỐNG (COMMUNICATION ARCHITECTURE)

Trong môi trường phân tán, các Microservices cần các giao thức chuẩn hóa để làm việc cùng nhau.

### 2.1 Giao Tiếp Đồng Bộ Hiệu Năng Cao (Synchronous Communication)
* **API Gateway làm Điểm Đầu Mối Duy Nhất (Single Entry Point):** Toàn bộ Request từ Client (Web, Mobile) phải đi qua API Gateway (ví dụ: YARP, Kong, Ocelot, AWS API Gateway). API Gateway chịu trách nhiệm: Định tuyến (Routing), Giới hạn băng thông (Rate Limiting), Xác thực tập trung (Authentication), và Chuyển đổi giao thức (nếu có).
* **Giao thức Giao tiếp:**
    * *Client-to-Service:* Sử dụng HTTP RESTful API hoặc GraphQL qua định dạng JSON.
    * *Service-to-Service (Khi bắt buộc cần dữ liệu ngay lập tức):* Sử dụng **gRPC** (giao thức nhị phân dựa trên HTTP/2 và Protocol Buffers) để tối ưu hóa hiệu năng, giảm độ trễ và giảm thiểu kích thước gói tin truyền tải qua mạng.

### 2.2 Giao Tiếp Bất Đồng Bộ Hướng Sự Kiện (Asynchronous Event-Driven)
Để đạt được trạng thái lỏng lẻo (Eventual Consistency), các dịch vụ phải giao tiếp chủ yếu qua cơ chế Publish/Subscribe.
* **Hạ tầng Message Broker:** Sử dụng một hệ thống hàng đợi tin nhắn tập trung như **RabbitMQ, Apache Kafka, hoặc AWS SQS/SNS**.
* **Sự kiện Tích hợp (Integration Events):** Khi một dịch vụ thay đổi trạng thái quan trọng (ví dụ: Đơn hàng được tạo), nó sẽ phát ra một sự kiện (Event) ra Broker. Các dịch vụ quan tâm đến sự kiện đó sẽ tự động tiêu thụ (Consume) và xử lý bất đồng bộ.

---

## 3. KIẾN TRÚC NỘI BỘ DỊCH VỤ VÀ THIẾT KẾ MIỀN (INTERNAL ARCHITECTURE)

Dù viết bằng bất kỳ ngôn ngữ nào, cấu trúc mã nguồn bên trong một service cần tuân thủ các nguyên lý cô lập logic nghiệp vụ.

### 3.1 Đóng Gói Theo Tính Năng (Feature Encapsulation)
* Khuyến khích áp dụng các kiến trúc hướng tính năng như **Vertical Slice Architecture** hoặc kiến trúc sạch (**Clean/Hexagonal Architecture**). 
* Mã nguồn phải được tổ chức sao cho khi một thay đổi về mặt nghiệp vụ xảy ra, lập trình viên chỉ cần chỉnh sửa trong phạm vi một phân vùng tính năng (Feature Folder / Bounded Context) thay vì sửa đổi dàn trải trên tất cả các tầng kỹ thuật (UI, Business Logic, Data Access).

### 3.2 Mô Hình Miền Giàu Có (Rich Domain Model) vs Mô Hình Thiếu Máu (Anemic Model)
* Các đối tượng thực thể (Entities/Aggregates) chịu trách nhiệm thực thi các quy tắc nghiệp vụ cốt lõi không được là các cấu trúc dữ liệu rỗng (chỉ có thuộc tính chứa dữ liệu). 
* Logic kiểm tra điều kiện nghiệp vụ và thay đổi trạng thái phải được đóng gói bên trong các phương thức của chính Entity đó để bảo toàn tính toàn vẹn của dữ liệu.

---

## 4. CƠ CHẾ CHỊU LỖI NÂNG CAO VÀ NHẤT QUÁN DỮ LIỆU (RESILIENCE & CONSISTENCY)

Hệ thống phân tán có tỷ lệ gặp lỗi mạng rất cao. Do đó, thiết kế hệ thống phải bao gồm năng lực tự phục hồi và bảo vệ chống sập dây chuyền.

### 4.1 Giải Quyết Bài Toán Dual-Write bằng Outbox Pattern
* **Thách thức:** Khi một dịch vụ thực hiện ghi dữ liệu vào DB nội bộ và gửi tin nhắn sang Message Broker, hành vi này không thể nằm chung một transaction của hệ thống phân tán. Nếu mạng lỗi sau khi DB ghi xong, tin nhắn sẽ bị mất.
* **Giải pháp (Outbox Pattern):** Integration Event không được gửi trực tiếp lên Broker. Nó phải được lưu vào một bảng tạm (Outbox Table) nằm trong cùng một Database Transaction của nghiệp vụ đó. Một tiến trình chạy ngầm (Background Worker) độc lập sẽ quét bảng Outbox này để đảm bảo gửi tin nhắn thành công lên Broker theo nguyên lý **At-least-once delivery** (Chuyển giao ít nhất một lần).

### 4.2 Pattern Kháng Lỗi (Fault Tolerance & Resilience)
Mọi cuộc gọi đồng bộ qua mạng (HTTP/gRPC) giữa các dịch vụ bắt buộc phải được bọc trong các cơ chế:
* **Retry Pattern:** Tự động thử lại cuộc gọi với thuật toán giãn cách thời gian tăng dần (Exponential Backoff) kết hợp nhiễu ngẫu nhiên (Jitter) để tránh làm nghẽn dịch vụ đích.
* **Circuit Breaker Pattern (Ngắt mạch):** Khi tỷ lệ gọi lỗi sang một dịch vụ khác vượt quá ngưỡng an toàn (ví dụ: 50% lỗi), mạch sẽ tự ngắt (Open). Mọi request tiếp theo sẽ bị từ chối ngay lập tức để bảo vệ hệ thống không bị cạn kiệt tài nguyên (Thread Pool), đồng thời cho dịch vụ đích có thời gian tự phục hồi.

---

## 5. BẢO MẬT PHI TẬP TRUNG (DISTRIBUTED SECURITY)

* **Identity Provider (IdP) Tập Trung:** Sử dụng một giải pháp quản lý định danh tập trung (như Keycloak, Auth0, Duende, hoặc Okta) để thực hiện chứng thực người dùng.
* **Xác thực không trạng thái (Stateless Authentication):** Sử dụng **JWT (JSON Web Token)** để truyền tải thông tin định danh và quyền hạn của người dùng.
* **Xác thực Phi tập trung (Decentralized Authorization):** API Gateway thực hiện kiểm tra tính hợp lệ của Token (Chữ ký, Hạn sử dụng). Sau đó, Token được chuyển tiếp (Forward) xuống các Microservices nội bộ. Từng Microservice sẽ tự giải mã Token để thực hiện phân quyền (RBAC/CBAC) tại chỗ mà không cần gọi ngược lại IdP, giúp giảm tải hệ thống.

---

## 6. TIÊU CHUẨN XỬ LÝ LỖI VÀ GIÁM SÁT TOÀN DIỆN (OBSERVABILITY)

Debug lỗi trong hệ thống Microservices là cực kỳ phức tạp vì một hành vi của người dùng có thể đi qua chuỗi nhiều dịch vụ khác nhau.

### 6.1 Chuẩn Hóa Định Dạng Lỗi Đầu Ra (Problem Details)
* Hệ thống phải thống nhất một cấu trúc phản hồi lỗi duy nhất cho toàn bộ các API (tương đương chuẩn **RFC 7807**). 
* Mã lỗi HTTP Status Code phải phản ánh đúng bản chất (400 Bad Request, 404 Not Found, 401 Unauthorized, 500 Internal Server Error). Dữ liệu lỗi trả về phải chứa thông tin mô tả rõ ràng, danh sách các trường vi phạm dữ liệu, và tuyệt đối không để lộ Stack Trace (mã nguồn lỗi hệ thống) ở môi trường Production.

### 6.2 Giám Sát Phân Tán (The Observability Triad)
Hệ thống bắt buộc phải tích hợp hạ tầng giám sát 3 thành phần:
1.  **Centralized Logging (Gom Log Tập Trung):** Toàn bộ Log của các dịch vụ phải là Structured Log (định dạng cấu trúc JSON, không dùng Plain Text thô) và được đẩy về một trung tâm lưu trữ (như ELK Stack, Splunk, Seq, Grafana Loki).
2.  **Distributed Tracing (Theo Vết Liên Tuyến):** Mỗi request đi vào hệ thống phải được cấp một mã định danh duy nhất (**CorrelationId / TraceId**). Mã này phải được đính kèm vào Header của mọi cuộc gọi mạng (HTTP/gRPC) hoặc Message chuyển qua Broker giữa các dịch vụ. Từ đó, quản trị viên có thể vẽ lại toàn bộ bản đồ luồng đi của request qua các dịch vụ khi có lỗi xảy ra (sử dụng OpenTelemetry, Jaeger, Zipkin).
3.  **Metrics & Health Checks:** Mỗi dịch vụ phải cung cấp endpoint kiểm tra tình trạng sức khỏe (`/healthz` hoặc `/live`, `/ready`). Hạ tầng giám sát (Prometheus + Grafana) sẽ định kỳ thu thập các thông số RAM, CPU, thời gian phản hồi (Latency) để đưa ra cảnh báo sớm.

---

## 7. QUY CHUẨN ĐÓNG GÓI, ĐIỀU PHỐI VÀ HẠ TẦNG (DEVOPS & INFRASTRUCTURE)

### 7.1 Containerization (Container hóa)
* Mọi dịch vụ bắt buộc phải được đóng gói thành một **Container Image** (sử dụng Docker hoặc Podman). Image này phải trải qua quy trình build đa tầng (Multi-stage build) để đảm bảo kích thước nhỏ gọn nhất và loại bỏ hoàn toàn các bộ công cụ phát triển (SDK) ra khỏi môi trường chạy thực tế (Runtime) nhằm tối ưu bảo mật.

### 7.2 Container Orchestration (Điều phối Container)
* Ở môi trường Phát triển (Development): Sử dụng **Docker-Compose** để định nghĩa và chạy toàn bộ hệ sinh thái dịch vụ (APIs, Databases, Message Brokers, Caches, Cổng bảo mật) chỉ bằng một câu lệnh duy nhất.
* Ở môi trường Production: Hệ thống phải sẵn sàng để triển khai trên các nền tảng điều phối container như **Kubernetes (K8s)** hoặc **Docker Swarm**. Các cấu hình về quản lý cấu hình tập trung (Centralized Configuration via ConfigMaps/Secrets) và tự động mở rộng (Horizontal Pod Autoscaling) phải được thiết lập riêng biệt khỏi mã nguồn ứng dụng.

### 7.3 Tự Động Hóa Dữ Liệu (Self-Healing Data Migrations)
* Ứng dụng khi khởi chạy trong môi trường Container phải tự động kiểm tra trạng thái của cơ sở dữ liệu mà nó sở hữu. Nếu cơ sở dữ liệu chưa tồn tại hoặc cấu trúc bảng cũ, dịch vụ phải tự động thực thi các tập lệnh thay đổi cấu trúc (Database Migrations) và nạp dữ liệu mẫu (Data Seeding) mà không cần sự can thiệp thủ công từ kỹ sư vận hành.

---

## 8. BẢNG ĐỐI CHIẾU CÔNG NGHỆ THAM KHẢO (TECHNOLOGY MAPPING)

Dưới đây là bảng gợi ý các công cụ/thư viện tương đương theo từng ngôn ngữ để hiện thực hóa bộ đặc tả yêu cầu trên:

| Thành phần kiến trúc | Hệ sinh thái .NET | Hệ sinh thái Java (Spring) | Hệ sinh thái Go | Hệ sinh thái Node.js (TypeScript) |
| :--- | :--- | :--- | :--- | :--- |
| **API Gateway** | YARP, Ocelot | Spring Cloud Gateway | Traefik, Kong, Envoy | KrakenD, Express Gateway |
| **Giao tiếp Đồng bộ** | gRPC-dotnet, HTTP | gRPC Java, Feign | gRPC-go | `@grpc/grpc-js` |
| **Message Broker** | RabbitMQ, Kafka | RabbitMQ, Kafka | RabbitMQ, Apache Kafka | RabbitMQ, KafkaJS |
| **Abstraction Messaging** | MassTransit, Rebus | Spring Cloud Stream | Watermill, Asynq | BullMQ, Amqp |
| **ORM / Data Access** | Entity Framework Core | Spring Data JPA (Hibernate) | GORM, Ent | TypeORM, Prisma |
| **Chịu lỗi (Resilience)** | Polly | Resilience4j | Go-resiliency, Sonybreaker | Cockatiel |
| **Background Jobs** | Quartz.NET, Hangfire | Spring Scheduler, Quartz | Enterprise Job Scheduler | Agenda, BullMQ |
| **Distributed Tracing** | OpenTelemetry | Spring Cloud Sleuth / OTel | OpenTelemetry Go | OpenTelemetry Node |
| **Centralized Logging** | Serilog -> Seq/ELK | Logback -> ELK / Graylog | Zap / ZeroLog -> Loki | Winston / Pino -> ELK |
| **Định dạng lỗi** | ProblemDetails Middleware | ControllerAdvice / RFC 7807 | Custom Error Middleware | Custom Error Middleware |

---
*Khuôn mẫu này đảm bảo tính nhất quán về mặt kiến trúc phần mềm cao cấp, đáp ứng đầy đủ các tiêu chuẩn vận hành thực tế của doanh nghiệp lẫn các tiêu chí chấm điểm chuyên sâu trong môi trường học thuật.*
