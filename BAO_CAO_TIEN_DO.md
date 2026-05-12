# Báo Cáo Tiến Độ Dự Án Chat Server Microservices (Phiên Bản 2 - Discord-like)

**Tài liệu tham chiếu:** `01_yeu_cau_phan_mem_version2.md`
**Ngày cập nhật:** 12/05/2026

---

## 1. Thành Quả & Những Module Đã Hoàn Thành

Đến hiện tại, team đã xây dựng thành công bộ khung kiến trúc Microservices cơ bản, thiết lập được môi trường Container hóa đồng bộ và hoàn thành được các dịch vụ cốt lõi:

*   **`common-lib`**: Đã hoàn thiện (100%), đóng vai trò thư viện dùng chung chứa DTO (`MessageDTO`, `LogEntry`), Enums, và công cụ bảo mật (`SecurityUtil` xử lý BCrypt).
*   **`log-service`**: Hoàn thiện (100%), tích hợp thành công RabbitMQ để nhận event, có chức năng ghi file và cung cấp REST API tra cứu log.
*   **`auth-service`**: Xử lý đăng ký, đăng nhập, bảo mật JWT và mã hóa mật khẩu.
*   **`messaging-service`**: Đã hoàn thiện toàn diện tính năng Chat (Version 2). Hỗ trợ Realtime qua Raw WebSocket, lưu trữ tin nhắn vào MySQL (Persistence), gửi tin theo phòng (Channel-based), các tính năng Edit/Delete tin nhắn, hiển thị Typing Indicator, và tích hợp bảo mật (kiểm tra quyền bằng OpenFeign gọi sang `server-service`).
*   **`channel-service`**: Quản lý Kênh/Phòng chat (CRUD, kết nối DB riêng, giao tiếp OpenFeign, async event bằng RabbitMQ).
*   **`server-service`**: Quản lý Server/Nhóm chat (Discord-like).
*   **`presence-service`**: Cập nhật trạng thái người dùng (Online/Offline).
*   **`gateway-service`**: Đóng vai trò cổng vào duy nhất, định tuyến request và kết nối WebSocket an toàn tới các dịch vụ phía sau.
*   **Cơ sở hạ tầng (Infrastructure)**: Khởi tạo hoàn tất `docker-compose.yml` với MySQL (Database), RabbitMQ (Message Broker) và MinIO (File Storage). Tích hợp thành công Docker network nội bộ (`chat-net`).

---

## 2. Cấu Trúc Các Port Hiện Tại

Hệ thống đang cấu hình các port theo kiến trúc container như sau:

### Hạ Tầng Dùng Chung (Infrastructure Services)
| Dịch Vụ | Internal Port (Docker) | Public Port (Host Machine) |
| :--- | :--- | :--- |
| **MySQL** | `3306` | `3307` |
| **RabbitMQ** (AMQP) | `5672` | `5672` |
| **RabbitMQ** (Management UI)| `15672` | `15672` |
| **MinIO** (S3 API) | `9000` | `9000` |
| **MinIO** (Console/Web UI) | `9001` | `9001` |

### Các Microservices
*(Các port này phục vụ cho việc định tuyến nội bộ, API Gateway nhận request từ Client ở cổng `8080`)*

| Microservice | Port Nội Bộ | Chức Năng |
| :--- | :--- | :--- |
| **Gateway Service** | `8080` | **Entry-point chính cho Client App** |
| **Auth Service** | `8081` | Quản lý danh tính và cấp phát JWT |
| **Messaging Service**| `8082` | Xử lý WebSocket Chat / Realtime |
| **Presence Service** | `8083` | Theo dõi và quản lý online/offline |
| **Log Service** | `8084` | Lắng nghe event và ghi lưu Audit |
| **Server Service** | `8085` | Quản lý Server/Group (như Discord Server) |
| **Channel Service** | `8086` | Quản lý các phân khu chat (Channel) |

---

## 3. Những Gì Cần Làm Tiếp Theo (Đối Chiếu Theo Version 2)

Dựa trên yêu cầu hệ thống (`01_yeu_cau_phan_mem_version2.md`), kiến trúc dự án tổng số cần 11 Microservices. Hiện tại chúng ta **còn thiếu và cần triển khai các phần sau**:

1.  **User Profile Service (Module B)**: Chưa có. Cần xây dựng để quản lý thông tin hiển thị (Avatar, Bio, Custom status) và tìm kiếm người dùng.
2.  **Role & Permission Service (Module F)**: Rất quan trọng. Hệ thống hiện chưa có kiểm soát phân quyền cụ thể (Owner, Admin, Moderator, Member) và tính năng Kick/Ban trong Server.
3.  **Notification Service (Module G)**: Tính năng thông báo chưa được xây dựng (@mention, đếm số tin nhắn chưa đọc, push notification realtime qua WebSocket).
4.  **File Service (Module H)**: Mặc dù Container MinIO đã hoạt động, chúng ta vẫn thiếu Backend File Service để xử lý upload ảnh/file đính kèm tối đa 10MB và tự sinh thumbnail.
5.  **Client App (Module L)**: Giao diện Java Swing kiểu Discord chưa được xây dựng.
6.  **Hoàn thiện sâu các module có sẵn**:
    *   `server-service`: Cần hoàn thiện hệ thống mã mời (Invite Code).

---

## 4. Quy Trình Build Dự Án & Test Từ A - Z (Mọi Góc Cạnh)

Để đảm bảo hệ thống vận hành trơn tru và phát hiện sớm các lỗi tích hợp, dưới đây là quy trình Build và Test toàn diện (dành cho mọi Developer và QA):

### A. Quy Trình Build Dự Án (Build Process)
1.  **Khởi động Hạ tầng Docker:**
    *   Mở Terminal tại thư mục root của project.
    *   Chạy lệnh: `docker compose up -d`
    *   Kiểm tra trạng thái: `docker compose ps` (Đảm bảo MySQL ở port 3307 và RabbitMQ ở port 15672 đang chạy ổn định).
2.  **Build Shared Library (`common-lib`):**
    *   Bắt buộc phải build `common-lib` trước tiên để các service khác có thể sử dụng DTO/Enums mới nhất.
    *   Lệnh: `.\mvnw.cmd -pl common-lib clean install -DskipTests` (trên Windows) hoặc `./mvnw -pl common-lib clean install -DskipTests` (trên Linux/Mac).
3.  **Build & Run Các Microservices:**
    *   Có thể chạy lần lượt các service cần thiết. Đặc biệt ưu tiên chạy `auth-service` trước tiên vì nó là điểm kiểm tra đầu vào.
    *   Lệnh (Ví dụ khởi động `auth-service`): `.\mvnw.cmd -pl auth-service spring-boot:run`
    *   Lệnh khởi động các service phụ thuộc khác:
        *   `.\mvnw.cmd -pl server-service spring-boot:run`
        *   `.\mvnw.cmd -pl messaging-service spring-boot:run`
    *   *(Mẹo: Hãy chú ý log khởi động, nếu service báo `Tomcat initialized with port...` và không có Exception đỏ thì đã chạy thành công).*

### B. Quy Trình Test Tích Hợp Toàn Diện Qua Postman (Testing Process)

Quy trình này mô phỏng luồng thao tác của người dùng cuối.

**1. Khởi tạo dữ liệu & Xác thực (REST API)**
*   **Test Đăng Ký (Register):** Gửi `POST http://localhost:8081/api/auth/register` với `{ "username": "testuser", "password": "123" }`. -> Kỳ vọng: `200 OK`.
*   **Test Đăng Nhập (Login):** Gửi `POST http://localhost:8081/api/auth/login` với thông tin trên. -> Kỳ vọng: Trả về JSON chứa `token` JWT. Lưu token này lại.

**2. Giao tiếp Thời gian thực (WebSocket Test)**
*   Mở New Connection dạng **WebSocket** trên Postman.
*   **Edge Case 1 (Token sai/Không có auth-service):** Thử kết nối `ws://localhost:8082/ws/chat?token=SAI_TOKEN`. -> Kỳ vọng: Bị văng mã lỗi `4001: Xác thực thất bại`.
*   **Luồng chuẩn:** Kết nối `ws://localhost:8082/ws/chat?token={TOKEN_THẬT}`. -> Kỳ vọng: Status `Connected` và nhận được message type `JOIN` từ server báo đã vào phòng.

**3. Test Tính Năng Chat Chi Tiết (Gửi Payload JSON qua WebSocket)**
*   **Edge Case 2 (Cố tình nhắn tin vào Server mình không thuộc về):**
    *   Gửi: `{ "type": "CHAT", "channelId": 999, "serverId": 999, "content": "Hack!" }`
    *   Kỳ vọng: Nhận về `ERROR` báo `"Không có quyền thực hiện thao tác trong server này"` (Vì `messaging-service` sẽ gọi qua `server-service` để check quyền thành viên).
*   **Test Chat chuẩn (Lưu ý phải thuộc Server đó):**
    *   Gửi: `{ "type": "CHAT", "channelId": 1, "serverId": 1, "content": "Hello" }`
    *   Kỳ vọng: Tin nhắn được lưu vào MySQL, Server phản hồi lại nguyên bản tin nhắn kèm theo thuộc tính `"messageId"`.
*   **Test Typing Indicator:**
    *   Gửi: `{ "type": "TYPING", "channelId": 1, "serverId": 1 }`
    *   Kỳ vọng: Lập tức broadcast cho channel mà không ghi DB.
*   **Test Edit & Delete:**
    *   Lấy `messageId` từ bước Chat trên. Gửi: `{ "type": "EDIT", "messageId": ID_ĐÓ, "content": "Sửa rồi" }`.
    *   Kỳ vọng: Database được update (`isEdited=true`) và bản tin được broadcast.
    *   Thử gửi: `{ "type": "DELETE", "messageId": ID_ĐÓ }`. Kỳ vọng: Bản ghi bị xóa khỏi MySQL.

**4. Test Lấy Lịch Sử (REST API Pagination)**
*   Gửi `GET http://localhost:8082/api/channels/1/messages?limit=10`.
*   Kỳ vọng: Trả về 10 tin nhắn mới nhất trong database, được sắp xếp thứ tự giảm dần.
*   **Edge Case 3 (Pagination):** Thêm query `?before={ID_TIN_NHẮN_CŨ_NHẤT}`. Kỳ vọng: Lấy tiếp 10 tin nhắn ra đời trước tin nhắn đó.

---

## 5. Những Lưu Ý Quan Trọng Khác

*   **⚠️ Cảnh Báo Trùng Port Trong Gateway Cấu Hình**: Trong cấu hình biến môi trường của Gateway (`docker-compose.yml`), `FILE_SERVICE_URL` và `LOG_SERVICE_URL` đều đang trỏ về port `8084`. Mặc dù dùng trong Docker có khác biệt hostname, nhưng khuyến nghị **gán cho File Service một Port mới (VD: 8087)** khi bắt đầu code module này.
*   **Sử dụng Chung Thư Viện (`common-lib`)**: Mọi DTO vận chuyển qua Message Queue **TUYỆT ĐỐI** chỉ khai báo tại `common-lib`. Nếu code trùng class ở service khác, hệ thống sẽ dính lỗi Deserialization do sai package name khi nhận message từ RabbitMQ.
*   **Vấn Đề Tương Thích Version**: Đặc biệt chú ý nếu sử dụng Java 25 (Phiên bản EA - Early Access), cấu hình `maven-compiler-plugin` kết hợp `Lombok` có thể văng lỗi không tìm thấy Getter/Setter. Cách tốt nhất để fix nhanh là chuyển qua code Getter/Setter thuần thay vì dùng các Annotation `@Data`, `@Getter` của Lombok.
*   **Độc Lập Database**: Database cho mỗi service phải độc lập hoàn toàn. Không viết câu query chéo (JOIN chéo) xuyên database. Mọi truy vấn liên kết phải dùng REST (qua OpenFeign) hoặc Message Event (RabbitMQ).
