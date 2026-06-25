# Chat Server Microservices (Discord Clone)

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![gRPC](https://img.shields.io/badge/gRPC-1.62-244C5A?style=for-the-badge&logo=grpc&logoColor=white)
![Keycloak](https://img.shields.io/badge/Keycloak-24-4D4D4D?style=for-the-badge&logo=keycloak&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)
![Kubernetes](https://img.shields.io/badge/Kubernetes-Kustomize-326CE5?style=for-the-badge&logo=kubernetes&logoColor=white)

Nền tảng nhắn tin thời gian thực (Real-time Chat Platform) lấy cảm hứng từ Discord. Hệ thống được xây dựng hoàn toàn trên kiến trúc **Microservices** với **Java 17** và hệ sinh thái **Spring Boot 3 / Spring Cloud**.

Dự án bao gồm **12 microservices nghiệp vụ**, 2 thư viện dùng chung (`common-lib`, `grpc-contracts`) và một ứng dụng Desktop Client phát triển bằng Java Swing — tổng cộng **15 Maven module**.

---

## Mục lục
1. [Tính năng nổi bật](#-tính-năng-nổi-bật)
2. [Kiến trúc hệ thống](#-kiến-trúc-hệ-thống)
3. [Công nghệ sử dụng](#-công-nghệ-sử-dụng)
4. [Danh sách Microservices](#-danh-sách-microservices)
5. [Hướng dẫn cài đặt & Triển khai](#-hướng-dẫn-cài-đặt--triển-khai)
6. [Ứng dụng Desktop Client](#-ứng-dụng-desktop-client)
7. [DevOps & Observability (Giám sát)](#-devops--observability)
8. [Cấu trúc thư mục](#-cấu-trúc-thư-mục)

---

## Tính năng nổi bật

* **Xác thực & Bảo mật:** Quản lý đăng ký/đăng nhập tập trung. Sử dụng băm mật khẩu BCrypt và JWT (Access Token 2h, Refresh Token 7 ngày). Hệ thống đang trong giai đoạn migrate sang **Keycloak (OIDC)**: Gateway xác thực ở chế độ **dual-mode** — ưu tiên token Keycloak (RS256/JWKS), tự động fallback về token HS256 do `auth-service` phát hành.
* **Hệ thống Máy chủ (Servers) & Kênh (Channels):** Tạo server cộng đồng, chia sẻ mã mời (invite code), tổ chức các kênh trò chuyện (text/voice).
* **Trò chuyện Thời gian thực (Real-time Chat):** Gửi/nhận tin nhắn độ trễ thấp qua WebSocket. Hỗ trợ: Sửa, xóa mềm, ghim, trả lời (reply) và thả cảm xúc (reactions).
* **Tin nhắn cá nhân (DM) & Bạn bè:** Quản lý danh sách bạn bè (gửi yêu cầu, chấp nhận, chặn) và nhắn tin riêng tư 1-1.
* **Trạng thái Hiện diện (Presence):** Cập nhật trạng thái người dùng theo thời gian thực (Online / Idle / Away / Do-Not-Disturb / Invisible).
* **Chia sẻ Tệp tin:** Tích hợp MinIO (chuẩn S3) để upload file (giới hạn 10MB) và tự động tạo hình thu nhỏ (thumbnail) cho hình ảnh.
* **Phân quyền RBAC:** Cấu hình vai trò linh hoạt trong từng server bằng cơ chế **permission bitmask** (Owner, Admin, Moderator...). Hỗ trợ Kick/Ban.
* **Thông báo thông minh:** Đánh dấu @mention, tính toán và hiển thị chính xác số lượng tin nhắn chưa đọc.
* **Kiểm toán (Audit Log):** Ghi nhận mọi hành động nhạy cảm qua message queue, gom log tập trung bằng Loki/Promtail.

---

## Kiến trúc hệ thống

```text
                    ┌──────────────┐
   Desktop Client → │   Gateway    │  (Spring Cloud Gateway + JwtAuthFilter)
   (Swing+FlatLaf)  │   :8080      │  Dual-mode JWT (Keycloak RS256 | HS256) → X-User-Id → Định tuyến
                    └──────┬───────┘
                           │
        ┌──────────────────┼─────────────────────────────┐
        │ gRPC (Đồng bộ, nội bộ – grpc-contracts/*.proto) │
   ┌────▼────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐   │
   │  auth   │  │ server  │  │ channel │  │   role   │ … │  12 Microservices
   └─────────┘  └─────────┘  └─────────┘  └──────────┘   │  (Mô hình Database-per-Service)
        │                                                 │
        │ Sự kiện (RabbitMQ Topic Exchange – Bất đồng bộ) │
        ▼  presence.status / notification.* / log.* ▼
   messaging ──WebSocket──> Client        notification / log
```

**Đặc điểm kiến trúc:**
- **Đồng bộ (nội bộ):** Các service gọi lẫn nhau qua **gRPC** (`net.devh/grpc-spring-boot-starter`), hợp đồng định nghĩa trong module `grpc-contracts` (`auth/server/channel/role/presence/user.proto`). Negotiation mặc định `plaintext` trong mạng nội bộ.
- **Bất đồng bộ:** Sử dụng **RabbitMQ Topic Exchange** (`chat.exchange`) để điều phối các luồng sự kiện như log, thông báo, trạng thái online.
- **Thời gian thực:** Quản lý kết nối **WebSocket** (`/ws/chat`) tại `messaging-service`, lưu phiên hoạt động qua `ConcurrentHashMap`.
- **Lưu trữ:** Áp dụng triệt để **Database-per-Service**, mỗi service sở hữu schema MySQL độc lập, tuyệt đối không JOIN chéo giữa các database.
- **Xác thực:** Gateway dùng `DualModeReactiveJwtDecoder` để verify Keycloak (RS256 qua JWKS) trước, fallback HS256 cho token cũ — phục vụ migrate dần sang Keycloak mà không gián đoạn client.

---

## Công nghệ sử dụng

| Phân lớp | Công nghệ áp dụng |
|---|---|
| **Ngôn ngữ / Runtime** | Java 17 (LTS) |
| **Framework lõi** | Spring Boot 3.2.4, Spring Cloud 2023.0.1 |
| **API Gateway** | Spring Cloud Gateway (WebFlux) |
| **Giao tiếp Dịch vụ (nội bộ)** | gRPC 1.62.2 + Protobuf 3.25.3 (`net.devh/grpc-spring-boot-starter`) |
| **Realtime** | Spring WebSocket |
| **Message Broker** | RabbitMQ 3 (Topic Exchange) |
| **Cơ sở dữ liệu** | MySQL 8.0 + Spring Data JPA / Hibernate |
| **Lưu trữ tệp (Object Storage)** | MinIO (S3) + Thumbnailator |
| **Xác thực / Bảo mật** | Keycloak 24 (OIDC), Spring Security, BCrypt, JJWT 0.12.5 (HS256 legacy) |
| **Giao diện Client** | Java Swing + FlatLaf 3.4 |
| **Build Tool** | Maven (Multi-module, 15 module), Lombok 1.18.34 |
| **DevOps & Triển khai** | Docker, Docker Compose, Kubernetes (Kustomize), Jenkins, SonarQube |
| **Giám sát (Observability)** | Prometheus, Grafana, Loki + Promtail, Spring Boot Actuator (Micrometer Tracing) |

---

## Danh sách Microservices

Hạ tầng chạy trên các port: `MySQL: 3307` | `RabbitMQ: 5672 (UI: 15672)` | `MinIO: 9000 (Console: 9001)` | `Keycloak: 8180`.

| Dịch vụ | Port | Database | Chức năng chính |
|---|---|---|---|
| `gateway-service` | 8080 | — | Cổng biên, xác thực dual-mode JWT, định tuyến request |
| `auth-service` | 8081 | `chat_auth_db` | Đăng ký, đăng nhập, cấp phát JWT & Refresh Token (HS256) |
| `messaging-service` | 8082 | `chat_messaging_db` | Xử lý chat realtime (WebSocket + RabbitMQ), reaction |
| `presence-service` | 8083 | `chat_presence_db` | Quản lý trạng thái hiện diện (Online, Offline,...) |
| `log-service` | 8084 | *(Loki/Promtail)* | Lắng nghe RabbitMQ và ghi Audit Log |
| `server-service` | 8085 | `chat_server_db` | Quản lý Server cộng đồng, thành viên, mã mời |
| `channel-service` | 8086 | `chat_channel_db` | Quản lý các kênh (Channels) và cấu hình kênh |
| `notification-service` | 8088 | `chat_notification_db` | Xử lý @mention, tính số tin nhắn chưa đọc |
| `file-service` | 8089 | `chat_file_db` | Quản lý upload/download MinIO, render thumbnail |
| `user-profile-service` | 8090 | `chat_profile_db` | Quản lý hồ sơ (Profile), Avatar người dùng |
| `role-service` | 8091 | `chat_role_db` | Phân quyền RBAC bằng bitmask, cấm (ban) |
| `friend-service` | 8092 | `chat_friend_db` | Quản lý danh sách, lời mời kết bạn |
| `common-lib` | — | — | DTO, Enum, Exception, Utility dùng chung |
| `grpc-contracts` | — | — | Định nghĩa file `.proto` & sinh stub gRPC dùng chung |
| `client-app` | — | — | Mã nguồn ứng dụng Desktop Client (Swing UI) |

---

## Hướng dẫn Cài đặt & Triển khai

### Yêu cầu hệ thống
- **JDK 17**
- **Maven 3.8+** (có thể dùng wrapper `./mvnw` đính kèm)
- **Docker** & **Docker Compose** (khuyến nghị để chạy hạ tầng mượt nhất)

### Bước 1: Cấu hình môi trường (.env)
Tạo file `.env` tại thư mục gốc. File này chứa cấu hình bảo mật, **tuyệt đối không commit lên Git**.

```env
# Mật khẩu cấu hình tự do theo ý bạn
JWT_SECRET=your-strong-secret-key-at-least-256-bits
MYSQL_ROOT_PASSWORD=your-mysql-password
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your-minio-password
# Keycloak
KC_DB_PASSWORD=keycloak_pass
KC_ADMIN_PASSWORD=admin
IMAGE_TAG=latest
```

### Bước 2: Khởi chạy hệ thống

**Cách 1: Chạy Full Stack bằng Docker Compose (Khuyên dùng)**
```bash
# 1. Build toàn bộ source code của 15 module
./mvnw clean package -DskipTests

# 2. Khởi tạo 12 services + Keycloak + hạ tầng (MySQL, RabbitMQ, MinIO)
docker compose up -d

# 3. Kiểm tra trạng thái chạy của các container
docker compose ps
docker compose logs -f gateway-service
```
API Gateway sẵn sàng tại `http://localhost:8080`. Keycloak Admin Console tại `http://localhost:8180` (realm `chatserver`).

**Cách 2: Chạy cục bộ từng Service (Dành cho Dev)**
*Lưu ý: Bạn phải tự dựng hạ tầng MySQL, RabbitMQ, MinIO, Keycloak trước và export các biến môi trường tương ứng.*
```bash
./mvnw -pl auth-service spring-boot:run
```

**Cách 3: Triển khai trên Kubernetes (Kustomize)**
Manifest tổ chức theo `base/` + `overlays/{dev,prod}`:
```bash
# Môi trường dev
kubectl apply -k k8s/overlays/dev

# Môi trường prod
kubectl apply -k k8s/overlays/prod
```

---

## Ứng dụng Desktop Client

Giao diện người dùng được xây dựng bằng **Java Swing** kết hợp thư viện **FlatLaf**, mang lại UI/UX hiện đại (giao diện 3 cột, hỗ trợ Light/Dark mode).

Để build và chạy Client:
```bash
# Đóng gói thành Fat-JAR
./mvnw -pl client-app clean package

# Chạy ứng dụng
java -jar client-app/target/client-app-*.jar
```
*Mặc định Client kết nối tới Gateway qua `http://localhost:8080` và WebSocket qua `ws://localhost:8080/ws/chat`. Có thể ghi đè khi chạy:*
```bash
java -Dchatsever.gateway.http=http://<host>:8080 \
     -Dchatsever.gateway.ws=ws://<host>:8080 \
     -jar client-app/target/client-app-*.jar
```

---

## DevOps & Observability

Toàn bộ công cụ DevOps và giám sát được tách riêng vào `docker-compose.devops.yml`:
```bash
docker compose -f docker-compose.devops.yml up -d
```

| Công cụ | Port | Vai trò & Chức năng |
|---|---|---|
| **Jenkins** | `9090` | Thực thi CI/CD pipeline (`Jenkinsfile`): Build → Đóng gói Docker Image → Push → Deploy. |
| **SonarQube** | `9002` | Rà soát và phân tích chất lượng mã nguồn tĩnh. |
| **Prometheus** | `9091` | Thu thập metrics từ endpoint `/actuator/prometheus` của các services (chu kỳ 10s). |
| **Grafana** | `3001` | Trực quan hóa metrics & logs thành các dashboard. |
| **Loki + Promtail** | — | Gom log tập trung từ toàn bộ container, truy vấn qua Grafana. |

*Lưu ý về thiết kế Container:* `Dockerfile.template` dùng base image `eclipse-temurin:17-jre-alpine`, chạy dưới quyền **non-root** và tối ưu JVM cho container (`-XX:+UseG1GC`, `-XX:MaxRAMPercentage`).

---

## Cấu trúc thư mục

```text
chat-server-microservices/
├── pom.xml                     # Root POM quản lý dependencies & 15 module
├── docker-compose.yml          # 12 microservices + Keycloak + hạ tầng (MySQL, Rabbit, MinIO)
├── docker-compose.devops.yml   # Bộ công cụ CI/CD & Giám sát
├── Dockerfile.template         # File mẫu tối ưu cho việc build image các service
├── Jenkinsfile                 # Cấu hình Pipeline CI/CD tự động
├── .env.example                # Mẫu file biến môi trường
├── common-lib/                 # DTO, Exceptions, Enums, Utils dùng chung
├── grpc-contracts/             # File .proto & stub gRPC dùng chung giữa các service
├── gateway-service/            # API Gateway & Spring Security (dual-mode JWT)
├── auth-service/               # Service xác thực, cấp token
├── messaging-service/          # Service xử lý chat, WebSocket
├── ...                         # (Các service nghiệp vụ khác)
├── client-app/                 # Source code Desktop Client (Swing UI)
├── k8s/                        # Manifest Kubernetes (Kustomize: base + overlays/dev,prod)
└── devops/                     # Cấu hình Prometheus, Grafana, Loki, Promtail, Keycloak realm
```
