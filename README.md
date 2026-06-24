# Chat Server Microservices

Nền tảng nhắn tin thời gian thực (Realtime Chat Platform) kiểu Discord, xây dựng theo
kiến trúc **Microservices** với **Java 17 + Spring Boot 3 / Spring Cloud**. Hệ thống gồm
**12 microservices** độc lập, một thư viện dùng chung và một ứng dụng Desktop client viết bằng Java Swing.

> Đồ án môn **SE330 – Ngôn ngữ lập trình JAVA**.

---

## Mục lục
- [Tính năng chính](#tính-năng-chính)
- [Kiến trúc tổng thể](#kiến-trúc-tổng-thể)
- [Công nghệ sử dụng](#công-nghệ-sử-dụng)
- [Danh sách dịch vụ](#danh-sách-dịch-vụ)
- [Yêu cầu môi trường](#yêu-cầu-môi-trường)
- [Cài đặt & Chạy](#cài-đặt--chạy)
- [Cấu hình môi trường (.env)](#cấu-hình-môi-trường-env)
- [Ứng dụng Client](#ứng-dụng-client)
- [DevOps & Giám sát](#devops--giám-sát)
- [Cấu trúc thư mục](#cấu-trúc-thư-mục)
- [Nhóm thực hiện](#nhóm-thực-hiện)

---

## Tính năng chính

- **Xác thực tập trung**: đăng ký/đăng nhập, mật khẩu hash BCrypt, JWT (access 2h + refresh 7 ngày).
- **Máy chủ & Kênh**: tạo server cộng đồng, mã mời (invite code), kênh trò chuyện (text/voice).
- **Chat thời gian thực**: tin nhắn tức thời qua WebSocket; sửa, xóa mềm, ghim, trả lời, thả cảm xúc (reaction).
- **Tin nhắn riêng (DM)** và **quản lý bạn bè** (kết bạn, chấp nhận, chặn).
- **Hiện diện (Presence)**: trạng thái Online / Idle / Away / Do-Not-Disturb / Invisible realtime.
- **Chia sẻ tệp**: upload qua MinIO (giới hạn 10MB), tự tạo thumbnail cho ảnh.
- **Phân quyền RBAC**: vai trò theo server với cơ chế **permission bitmask** (Owner/Admin/Moderator…), kick/ban.
- **Thông báo**: @mention, đếm tin chưa đọc (unread counter).
- **Kiểm toán (Audit log)** mọi sự kiện qua hàng đợi.

## Kiến trúc tổng thể

```
                    ┌──────────────┐
   Desktop Client → │   Gateway    │  (Spring Cloud Gateway + JwtAuthFilter)
   (Swing+FlatLaf)  │   :8080      │  xác thực JWT → inject X-User-Id → định tuyến
                    └──────┬───────┘
                           │
        ┌──────────────────┼─────────────────────────────┐
        │ REST (OpenFeign – đồng bộ)                      │
   ┌────▼────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐   │
   │  auth   │  │ server  │  │ channel │  │   role   │ … │  12 microservices
   └─────────┘  └─────────┘  └─────────┘  └──────────┘   │  (Database-per-Service)
        │                                                 │
        │ Events (RabbitMQ Topic Exchange – bất đồng bộ)  │
        ▼  presence.status / notification.* / log.*       ▼
   messaging ──WebSocket──> Client        notification / log
```

- **Giao tiếp đồng bộ**: dịch vụ gọi nhau bằng **Spring Cloud OpenFeign** (`@FeignClient`).
- **Giao tiếp bất đồng bộ**: **RabbitMQ Topic Exchange** (`chat.exchange`) cho sự kiện presence/notification/log.
- **Realtime**: **WebSocket** (`/ws/chat`), quản lý phiên bằng `ConcurrentHashMap`.
- **Dữ liệu**: mỗi service một schema MySQL riêng (**Database-per-Service**), không JOIN chéo.

## Công nghệ sử dụng

| Lĩnh vực | Công nghệ |
|---|---|
| Ngôn ngữ / Runtime | Java 17 (LTS) |
| Framework | Spring Boot 3.2.4, Spring Cloud 2023.0.1 |
| API Gateway | Spring Cloud Gateway |
| Giao tiếp dịch vụ | Spring Cloud OpenFeign |
| Realtime | Spring WebSocket |
| Message Broker | RabbitMQ 3 |
| CSDL | MySQL 8.0 + Spring Data JPA / Hibernate |
| Lưu trữ tệp | MinIO (S3) + Thumbnailator |
| Bảo mật | Spring Security, BCrypt, JWT (JJWT 0.12.5) |
| Client | Java Swing + FlatLaf 3.4 |
| Build | Maven (đa mô-đun), Lombok 1.18.30 |
| DevOps | Docker, Docker Compose, Jenkins |
| Giám sát | Prometheus + Grafana + Spring Boot Actuator (Micrometer) |

## Danh sách dịch vụ

| Service | Cổng | Database | Vai trò |
|---|---|---|---|
| `gateway-service` | 8080 | — | Cổng biên, xác thực JWT, định tuyến |
| `auth-service` | 8081 | `chat_auth_db` | Đăng ký/đăng nhập, JWT, refresh token |
| `messaging-service` | 8082 | `chat_messaging_db` | Chat realtime (WebSocket + RabbitMQ), reaction |
| `presence-service` | 8083 | `chat_presence_db` | Trạng thái hiện diện |
| `log-service` | 8084 | (file) | Ghi log kiểm toán (consumer RabbitMQ) |
| `server-service` | 8085 | `chat_server_db` | Server, thành viên, mã mời |
| `channel-service` | 8086 | `chat_channel_db` | Kênh, cấu hình kênh |
| `notification-service` | 8088 | `chat_notification_db` | @mention, đếm chưa đọc |
| `file-service` | 8089 | `chat_file_db` | Upload/download MinIO, thumbnail |
| `user-profile-service` | 8090 | `chat_profile_db` | Hồ sơ người dùng |
| `role-service` | 8091 | `chat_role_db` | Phân quyền RBAC bitmask, ban |
| `friend-service` | 8092 | `chat_friend_db` | Quan hệ bạn bè |
| `common-lib` | — | — | DTO / Enum / Util dùng chung |
| `client-app` | — | — | Ứng dụng Desktop (Swing) |

**Hạ tầng**: MySQL `:3307` · RabbitMQ `:5672` (UI `:15672`) · MinIO `:9000` (Console `:9001`).

## Yêu cầu môi trường

- **JDK 17**
- **Maven 3.8+** (hoặc dùng `./mvnw` đi kèm)
- **Docker** & **Docker Compose** (khuyến nghị để chạy toàn hệ thống)

## Cài đặt & Chạy

### Cách 1 — Docker Compose (khuyến nghị)

```bash
# 1. Tạo file .env ở thư mục gốc (xem mục Cấu hình bên dưới)

# 2. Build toàn bộ mô-đun
./mvnw clean package -DskipTests

# 3. Khởi chạy 12 service + MySQL + RabbitMQ + MinIO
docker compose up -d

# 4. Theo dõi log / kiểm tra trạng thái
docker compose ps
docker compose logs -f gateway-service
```

Gateway sẵn sàng tại **http://localhost:8080**.

### Cách 2 — Chạy một service cục bộ (dev)

```bash
./mvnw -pl auth-service spring-boot:run
```

> Khi chạy cục bộ, cần tự bật MySQL/RabbitMQ/MinIO và đặt biến môi trường tương ứng.

## Cấu hình môi trường (.env)

Tạo file `.env` tại thư mục gốc với các biến sau:

```env
# JWT
JWT_SECRET=your-strong-secret-key

# MySQL
MYSQL_ROOT_PASSWORD=your-mysql-password
MYSQL_DATABASE=chat_auth_db

# RabbitMQ
RABBITMQ_DEFAULT_USER=guest
RABBITMQ_DEFAULT_PASS=guest

# MinIO
MINIO_ROOT_USER=minioadmin
MINIO_ROOT_PASSWORD=your-minio-password

# Docker image tag (tùy chọn)
IMAGE_TAG=latest
```

> **Không commit file `.env`** chứa thông tin nhạy cảm lên Git.

## Ứng dụng Client

Ứng dụng Desktop (`client-app`) viết bằng **Java Swing + FlatLaf** (giao diện ba cột, hỗ trợ chế độ Sáng/Tối).

```bash
# Build fat-JAR
./mvnw -pl client-app clean package

# Chạy
java -jar client-app/target/client-app-*.jar
```

Client kết nối tới Gateway tại `http://localhost:8080` và WebSocket `ws://localhost:8080/ws/chat`.

## DevOps & Giám sát

Stack DevOps tách riêng trong `docker-compose.devops.yml`:

```bash
docker compose -f docker-compose.devops.yml up -d
```

| Công cụ | Cổng | Mục đích |
|---|---|---|
| Jenkins | 9090 | CI/CD pipeline (`Jenkinsfile`): build → đóng gói 12 image → push → deploy |
| SonarQube | 9002 | Phân tích chất lượng mã tĩnh |
| Prometheus | 9091 | Thu thập metrics từ `/actuator/prometheus` (scrape 10s) |
| Grafana | 3001 | Trực quan hóa metrics |

Mỗi service phơi điểm cuối **Spring Boot Actuator** (`/actuator/health`, `/actuator/prometheus`).
`Dockerfile.template` dùng base `eclipse-temurin:17-jre-alpine`, chạy **non-root**, JVM tối ưu container (G1GC, MaxRAMPercentage).

## Cấu trúc thư mục

```
chat-server-microservices/
├─ pom.xml                     # POM cha quản lý 14 mô-đun
├─ docker-compose.yml          # 12 service + hạ tầng
├─ docker-compose.devops.yml   # Jenkins, SonarQube, Prometheus, Grafana
├─ Dockerfile.template         # Khuôn mẫu image dùng chung
├─ Jenkinsfile                 # Pipeline CI/CD
├─ common-lib/                 # DTO / Enum / Util dùng chung
├─ gateway-service/            # API Gateway
├─ auth-service/               # Xác thực JWT
├─ ...                         # các microservice nghiệp vụ
├─ client-app/                 # Ứng dụng Desktop (Swing + FlatLaf)
└─ devops/                     # Cấu hình Prometheus / Grafana
```

## Nhóm thực hiện

| Họ và tên | MSSV |
|---|---|
| Tô Thành Nguyên | 24521207 |
| Lê Vũ Hoàng Nguyên | 24521182 |
| Nguyễn Quốc Nguyên | 24521197 |
| Nguyễn Thành Phát | 24521310 |
