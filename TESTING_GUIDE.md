# Hướng Dẫn Chạy Và Kiểm Thử (Testing Guide) Hệ Thống Microservices

Tài liệu này hướng dẫn cách khởi chạy và kiểm thử các chức năng của hệ thống Chat Server Microservices.

## 1. Yêu cầu hệ thống (Prerequisites)
- **Java 17+**: JDK 17 hoặc mới hơn.
- **Maven**: Để build dự án (có thể sử dụng `mvnw` đi kèm).
- **Docker & Docker Compose**: Để chạy các database và infrastructure (MySQL, RabbitMQ, MinIO,...).
- **Postman**: Công cụ để test các REST API và WebSocket.

## 2. Khởi chạy Infrastructure
Hệ thống yêu cầu các thành phần nền tảng như database, message broker. Sử dụng Docker Compose để khởi chạy nhanh:

```bash
# Di chuyển vào thư mục gốc của dự án
cd e:/source/repos/chat-server-microservices

# Khởi chạy infrastructure dưới background
docker-compose up -d
```
*Lưu ý: Mở Docker Desktop trước khi chạy lệnh này. Bạn có thể dùng lệnh `docker-compose ps` để kiểm tra xem các container đã chạy thành công hay chưa.*

## 3. Build và Khởi chạy Microservices

### 3.1. Cài đặt Common-Lib
Trước tiên, bạn cần build và cài đặt module `common-lib` vào local Maven repository vì các service khác phụ thuộc vào nó.

```bash
./mvnw clean install -pl common-lib -am
```

### 3.2. Khởi chạy các Services
Khởi động tuần tự hoặc song song các dịch vụ. Có thể chạy qua IDE (IntelliJ, Eclipse, VSCode) bằng cách chạy class chứa `main()` có annotation `@SpringBootApplication`, hoặc chạy lệnh sau qua terminal:

**Chạy Gateway (Bắt buộc để định tuyến)**
```bash
./mvnw spring-boot:run -pl gateway-service
```

**Chạy các Service cốt lõi (Mở terminal riêng cho từng service)**
```bash
./mvnw spring-boot:run -pl auth-service
./mvnw spring-boot:run -pl server-service
./mvnw spring-boot:run -pl channel-service
./mvnw spring-boot:run -pl messaging-service
./mvnw spring-boot:run -pl presence-service
```
*(Nếu bạn cấu hình discovery server như Eureka, hãy đảm bảo khởi động nó trước tiên).*

## 4. Hướng dẫn Test API qua Postman

Mọi request API từ client nên đi qua **API Gateway** (thường chạy ở port `8080`, vui lòng kiểm tra lại cấu hình `application.yml` của `gateway-service` để biết chính xác).

### Bước 1: Đăng ký / Đăng nhập để lấy Token (Auth Service)
1. **Đăng ký (Register)**
   - **Method:** POST
   - **URL:** `http://localhost:8080/api/auth/register`
   - **Body (JSON):**
     ```json
     {
       "username": "testuser",
       "email": "test@example.com",
       "password": "password123"
     }
     ```
2. **Đăng nhập (Login)**
   - **Method:** POST
   - **URL:** `http://localhost:8080/api/auth/login`
   - **Body (JSON):**
     ```json
     {
       "username": "testuser",
       "password": "password123"
     }
     ```
   - **Kết quả:** Bạn sẽ nhận được 1 chuỗi `accessToken` (JWT). Hãy copy chuỗi này.

### Bước 2: Test các API yêu cầu xác thực (ví dụ: Tạo Server/Channel)
Để gọi các API này, bạn cần gửi kèm token vừa lấy được ở trên.
- Trong Postman, sang tab **Authorization**.
- Chọn Type là **Bearer Token**.
- Dán chuỗi `accessToken` vào ô Token.

**Ví dụ: Tạo một Server mới (Server Service)**
- **Method:** POST
- **URL:** `http://localhost:8080/api/servers`
- **Body (JSON):**
  ```json
  {
    "name": "My Chat Server",
    "description": "Server chat test",
    "icon": "icon-url.png"
  }
  ```

## 5. Hướng dẫn Test WebSocket (Messaging Service)

Messaging Service xử lý chat realtime qua WebSocket. **Luôn kết nối qua API Gateway (port 8080)**, không truy cập trực tiếp vào messaging-service.
1. Mở một tab mới trong Postman, chọn **New** -> **WebSocket**.
2. **URL kết nối:** `ws://localhost:8080/ws/chat` *(Gateway sẽ proxy WS vào messaging-service — yêu cầu G2)*.
3. Truyền JWT Token qua query parameter để xác thực:
   - `ws://localhost:8080/ws/chat?token=YOUR_JWT_TOKEN`
4. Ấn **Connect**.
5. Sau khi kết nối thành công, bạn có thể gửi thử các message định dạng JSON (tuỳ thuộc vào format quy định trong `ChatWebSocketHandler`).
   - **Ví dụ Payload:**
     ```json
     {
       "type": "CHAT",
       "channelId": "1",
       "content": "hello"
     }
     ```

## 6. Xử lý các lỗi thường gặp
- **Lỗi kết nối CSDL (Connection Refused):** Đảm bảo docker-compose đã chạy và MySQL/RabbitMQ đã khởi động thành công.
- **Lỗi 401 Unauthorized:** Token đã hết hạn hoặc chưa cấu hình đúng `Authorization` header trên Postman. Hãy login lại để lấy token mới.
- **Lỗi ClassNotFoundException cho common-lib:** Bạn quên chạy bước 3.1 (`mvn clean install -pl common-lib`).
