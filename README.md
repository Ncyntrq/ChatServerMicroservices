# Chat Server Microservices (Discord Clone)

![Java](https://img.shields.io/badge/Java-17-ED8B00?style=for-the-badge&logo=java&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.2.4-6DB33F?style=for-the-badge&logo=spring-boot&logoColor=white)
![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1?style=for-the-badge&logo=mysql&logoColor=white)
![RabbitMQ](https://img.shields.io/badge/RabbitMQ-3-FF6600?style=for-the-badge&logo=rabbitmq&logoColor=white)
![Docker](https://img.shields.io/badge/Docker-Enabled-2496ED?style=for-the-badge&logo=docker&logoColor=white)

Nền tảng nhắn tin thời gian thực (Real-time Chat Platform) lấy cảm hứng từ Discord. Hệ thống được xây dựng hoàn toàn trên kiến trúc **Microservices** với **Java 17** và hệ sinh thái **Spring Boot 3 / Spring Cloud**. 

Dự án bao gồm **12 microservices** hoạt động độc lập, một thư viện dùng chung (Common Library) và một ứng dụng Desktop Client được phát triển bằng Java Swing.

---

## 📑 Mục lục
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

* **Xác thực & Bảo mật:** Quản lý đăng ký/đăng nhập tập trung. Sử dụng thuật toán băm mật khẩu BCrypt và chuẩn bảo mật JWT (Access Token tồn tại 2h, Refresh Token tồn tại 7 ngày).
* **Hệ thống Máy chủ (Servers) & Kênh (Channels):** Cho phép người dùng tạo server cộng đồng, chia sẻ mã mời (invite code) và tổ chức các kênh trò chuyện (text/voice).
* **Trò chuyện Thời gian thực (Real-time Chat):** Gửi/nhận tin nhắn độ trễ thấp qua WebSocket. Hỗ trợ các thao tác: Sửa, xóa mềm, ghim tin nhắn, trả lời (reply) và thả cảm xúc (reactions).
* **Tin nhắn cá nhân (DM) & Bạn bè:** Quản lý danh sách bạn bè (gửi yêu cầu, chấp nhận, chặn) và nhắn tin riêng tư 1-1.
* **Trạng thái Hiện diện (Presence):** Cập nhật trạng thái người dùng theo thời gian thực (Online / Idle / Away / Do-Not-Disturb / Invisible).
* **Chia sẻ Tệp tin:** Tích hợp MinIO (chuẩn S3) để upload file (giới hạn 10MB) và tự động tạo hình thu nhỏ (thumbnail) cho hình ảnh.
* **Phân quyền RBAC:** Cấu hình vai trò linh hoạt trong từng server sử dụng cơ chế **permission bitmask** (Owner, Admin, Moderator...). Hỗ trợ tính năng Kick/Ban.
* **Thông báo thông minh:** Đánh dấu @mention, tính toán và hiển thị chính xác số lượng tin nhắn chưa đọc.
* **Kiểm toán (Audit Log):** Ghi nhận mọi hành động nhạy cảm trong hệ thống qua message queue để lưu trữ và truy xuất khi cần.

---

## Kiến trúc hệ thống

```text
                    ┌──────────────┐
   Desktop Client → │   Gateway    │  (Spring Cloud Gateway + JwtAuthFilter)
   (Swing+FlatLaf)  │   :8080      │  Xác thực JWT → Phân tích X-User-Id → Định tuyến
                    └──────┬───────┘
                           │
        ┌──────────────────┼─────────────────────────────┐
        │ REST API (OpenFeign – Đồng bộ)                  │
   ┌────▼────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐   │
   │  auth   │  │ server  │  │ channel │  │   role   │ … │  12 Microservices
   └─────────┘  └─────────┘  └─────────┘  └──────────┘   │  (Mô hình Database-per-Service)
        │                                                 │
        │ Sự kiện (RabbitMQ Topic Exchange – Bất đồng bộ) │
        ▼  presence.status / notification.* / log.* ▼
   messaging ──WebSocket──> Client        notification / log
```

**Đặc điểm kiến trúc:**
- **Đồng bộ:** Các service gọi nội bộ với nhau thông qua **Spring Cloud OpenFeign** (`@FeignClient`).
- **Bất đồng bộ:** Sử dụng **RabbitMQ Topic Exchange** (`chat.exchange`) để điều phối các luồng sự kiện như log, thông báo, trạng thái online.
- **Thời gian thực:** Quản lý các kết nối **WebSocket** (`/ws/chat`), lưu trữ phiên hoạt động qua `ConcurrentHashMap`.
- **Lưu trữ:** Áp dụng triệt để mô hình **Database-per-Service**, mỗi service sở hữu một schema MySQL độc lập, tuyệt đối không JOIN chéo giữa các database.

---

## Công nghệ sử dụng

| Phân lớp | Công nghệ áp dụng |
|---|---|
| **Ngôn ngữ / Runtime** | Java 17 (LTS) |
| **Framework lõi** | Spring Boot 3.2.4, Spring Cloud 2023.0.1 |
| **API Gateway** | Spring Cloud Gateway |
| **Giao tiếp Dịch vụ** | Spring Cloud OpenFeign |
| **Realtime** | Spring WebSocket |
| **Message Broker** | RabbitMQ 3 |
| **Cơ sở dữ liệu** | MySQL 8.0 + Spring Data JPA / Hibernate |
| **Lưu trữ tệp (Object Storage)**| MinIO (S3) + Thumbnailator |
| **Bảo mật** | Spring Security, BCrypt, JJWT 0.12.5 |
| **Giao diện Client** | Java Swing + FlatLaf 3.4 |
| **Build Tool** | Maven (Multi-module), Lombok 1.18.30 |
| **DevOps & Triển khai** | Docker, Docker Compose, Jenkins |
| **Giám sát (Observability)** | Prometheus, Grafana, Spring Boot Actuator (Micrometer) |

---

## Danh sách Microservices

Hệ thống hạ tầng chạy trên các port: `MySQL: 3307` | `RabbitMQ: 5672 (UI: 15672)` | `MinIO: 9000 (Console: 9001)`.

| Dịch vụ | Port | Database | Chức năng chính |
|---|---|---|---|
| `gateway-service` | 8080 | — | Cổng biên, xác thực token JWT, định tuyến request |
| `auth-service` | 8081 | `chat_auth_db` | Đăng ký, đăng nhập, cấp phát JWT & Refresh Token |
| `messaging-service` | 8082 | `chat_messaging_db` | Xử lý logic Chat realtime (WebSocket + RabbitMQ), reaction |
| `presence-service` | 8083 | `chat_presence_db` | Quản lý trạng thái hiện diện (Online, Offline,...) |
| `log-service` | 8084 | *(Ghi ra file)* | Lắng nghe RabbitMQ và ghi Audit Log |
| `server-service` | 8085 | `chat_server_db` | Quản lý thông tin Server cộng đồng, thành viên, mã mời |
| `channel-service` | 8086 | `chat_channel_db` | Quản lý các kênh (Channels) và cấu hình kênh |
| `notification-service` | 8088 | `chat_notification_db` | Xử lý @mention, tính toán số tin nhắn chưa đọc |
| `file-service` | 8089 | `chat_file_db` | Quản lý upload/download MinIO, render thumbnail |
| `user-profile-service` | 8090 | `chat_profile_db` | Quản lý hồ sơ (Profile), Avatar người dùng |
| `role-service` | 8091 | `chat_role_db` | Xử lý logic phân quyền RBAC bằng bitmask, cấm (ban) |
| `friend-service` | 8092 | `chat_friend_db` | Quản lý danh sách, lời mời kết bạn |
| `common-lib` | — | — | Chứa các DTO, Enum, Utility dùng chung cho mọi service |
| `client-app` | — | — | Mã nguồn ứng dụng Desktop Client |

---

## Hướng dẫn Cài đặt & Triển khai

### Yêu cầu hệ thống
- **JDK 17**
- **Maven 3.8+** (Có thể dùng wrapper `./mvnw` đính kèm)
- **Docker** & **Docker Compose** (Khuyến nghị để chạy hạ tầng mượt mà nhất)

### Bước 1: Cấu hình môi trường (.env)
Tạo file `.env` tại thư mục gốc của dự án. File này chứa các cấu hình bảo mật, **tuyệt đối không commit lên Git**.

```env
# Mật khẩu cấu hình tự do theo ý bạn
JWT_SECRET=your-strong-secret-key-at-least-256-bits
MYSQL_ROOT_PASSWORD=your-mysql-password
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your-minio-password
IMAGE_TAG=latest
```

### Bước 2: Khởi chạy hệ thống

**Cách 1: Chạy Full Stack bằng Docker Compose (Khuyên dùng)**
```bash
# 1. Build toàn bộ source code của 14 module
./mvnw clean package -DskipTests

# 2. Khởi tạo 12 services + cơ sở hạ tầng (MySQL, RabbitMQ, MinIO)
docker compose up -d

# 3. Kiểm tra trạng thái chạy của các container
docker compose ps
docker compose logs -f gateway-service
```
API Gateway sẽ sẵn sàng đón request tại: `http://localhost:8080`

**Cách 2: Chạy cục bộ từng Service (Dành cho Dev)**
*Lưu ý: Bạn phải tự dựng hạ tầng MySQL, RabbitMQ, MinIO trước và export các biến môi trường tương ứng.*
```bash
./mvnw -pl auth-service spring-boot:run
```

---

## Ứng dụng Desktop Client

Giao diện người dùng được xây dựng bằng **Java Swing** kết hợp với thư viện **FlatLaf** mang lại UI/UX hiện đại (giao diện 3 cột, hỗ trợ Light/Dark mode).

Để build và chạy Client:
```bash
# Đóng gói thành Fat-JAR
./mvnw -pl client-app clean package

# Chạy ứng dụng
java -jar client-app/target/client-app-*.jar
```
*Lưu ý: Client được cấu hình mặc định kết nối tới Gateway qua `http://localhost:8080` và WebSocket qua `ws://localhost:8080/ws/chat`.*

---

## DevOps & Observability

Toàn bộ công cụ phục vụ DevOps và giám sát hệ thống được tách riêng vào file `docker-compose.devops.yml`. Khởi chạy bằng lệnh:
```bash
docker compose -f docker-compose.devops.yml up -d
```

| Công cụ | Port | Vai trò & Chức năng |
|---|---|---|
| **Jenkins** | `9090` | Thực thi CI/CD pipeline (định nghĩa trong `Jenkinsfile`): Build → Đóng gói Docker Image → Push → Deploy. |
| **SonarQube** | `9002` | Rà soát và phân tích chất lượng mã nguồn tĩnh. |
| **Prometheus** | `9091` | Thu thập metrics liên tục từ endpoint `/actuator/prometheus` của các services (Chu kỳ 10s). |
| **Grafana** | `3001` | Trực quan hóa dữ liệu giám sát thành các biểu đồ dashboard. |

*Lưu ý về thiết kế Container:* File `Dockerfile.template` sử dụng base image `eclipse-temurin:17-jre-alpine`, thiết lập chạy dưới quyền **non-root** và tối ưu JVM cho môi trường container (`-XX:+UseG1GC`, `-XX:MaxRAMPercentage`).

---

## Cấu trúc thư mục

```text
chat-server-microservices/
├── pom.xml                     # Root POM quản lý toàn bộ dependencies & 14 modules
├── docker-compose.yml          # Triển khai 12 microservices + hạ tầng (MySQL, Rabbit, MinIO)
├── docker-compose.devops.yml   # Triển khai bộ công cụ CI/CD & Giám sát
├── Dockerfile.template         # File mẫu tối ưu cho việc build image các service
├── Jenkinsfile                 # Cấu hình Pipeline CI/CD tự động
├── .env.example                # Mẫu file biến môi trường
├── common-lib/                 # Chứa DTO, Exceptions, Enums, Utils dùng chung
├── gateway-service/            # API Gateway & Spring Security
├── auth-service/               # Service xác thực, cấp token
├── messaging-service/          # Service xử lý chat, WebSocket
├── ...                         # (Các service nghiệp vụ khác)
├── client-app/                 # Source code Desktop Client (Swing UI)
└── devops/                     # Thư mục chứa cấu hình cho Prometheus, Grafana, Loki...
