# Hướng dẫn Refactor và Kiểm thử Messaging Service

Tài liệu này tóm tắt các thay đổi lớn về kiến trúc và các cải tiến đã được thực hiện đối với `messaging-service` qua ba giai đoạn riêng biệt, kèm theo đó là hướng dẫn chi tiết về cách kiểm thử các thay đổi này cả bằng phương pháp thủ công và tự động.

---

## 🏗️ Tóm tắt các Thay đổi Kiến trúc

### Giai đoạn 1: Độ tin cậy & Hỗ trợ Optimistic UI
- **Transactional Outbox Pattern**: Triển khai Entity `OutboxMessage` để đảm bảo rằng việc ghi vào cơ sở dữ liệu (lưu tin nhắn chat) và đẩy sự kiện lên message broker (gửi tới RabbitMQ) diễn ra đồng thời (atomic). Điều này ngăn chặn tình trạng "tin nhắn ma" khi lưu database thành công nhưng đẩy lên RabbitMQ thất bại (hoặc ngược lại).
- **Loại bỏ tin nhắn trùng lặp (Message Deduplication)**: Bổ sung ID tin nhắn duy nhất được tạo từ phía client. Nếu frontend gửi cùng một tin nhắn hai lần (do kết nối mạng thử lại hoặc do độ trễ của Optimistic UI), backend sẽ bỏ qua tin nhắn trùng lặp, đảm bảo tính nhất quán của dữ liệu.
- **Hỗ trợ Optimistic UI**: Frontend giờ đây có thể hiển thị tin nhắn ngay lập tức trong khi hệ thống đang xử lý ngầm, và tự động cập nhật trạng thái từ "Đang gửi..." thành "Đã gửi" sau khi nhận được xác nhận từ backend.

### Giai đoạn 2: Vertical Slice Architecture & Domain-Driven Design (DDD)
- **Chuyển đổi từ N-Tier sang Vertical Slices**: Từ bỏ kiến trúc phân lớp truyền thống (`Controller -> Service -> Repository`) để chuyển sang kiến trúc dựa trên tính năng (feature-based). Code hiện tại được nhóm theo Domain (`domain/`), các feature handler (`features/`), và hạ tầng cơ sở (`infrastructure/`).
- **Rich Domain Models (Mô hình Domain Phong phú)**: Biến đổi Entity `ChatMessage` từ dạng Anemic (thiếu logic) thành một Rich Domain Model. Nó tự đóng gói các quy tắc nghiệp vụ của chính mình (ví dụ: các phương thức `edit()` và `delete()` tự động thực thi các quy tắc về trạng thái) thay vì phụ thuộc vào các service bên ngoài.
- **Value Objects**: Giới thiệu `MessageContent` và `UserId` để xác thực dữ liệu ngay khoảnh khắc nó bước vào hệ thống. Một đối tượng `MessageContent` mặc định không thể rỗng hoặc vượt quá 4000 ký tự, điều này có nghĩa là core logic của chúng ta không bao giờ phải kiểm tra lỗi null.
- **Event-Driven Handlers**: Tạo ra các handler cụ thể như `ChatCommandHandler` và `ChatQueryHandler` để phân tách hoàn toàn các thao tác ghi (commands) và thao tác đọc (queries).

### Giai đoạn 3: Kiểm thử Tự động & Quản lý Chất lượng
- **Unit Testing Toàn diện**: Tạo các bộ test chuyên dụng cho các Domain Entity (`ChatMessageTest`, `OutboxMessageTest`) và Value Objects (`MessageContentTest`, `UserIdTest`) để chứng minh bằng toán học rằng các quy tắc nghiệp vụ hoạt động đúng trong môi trường cô lập.
- **Integration Testing với Mockito**: Viết `ChatCommandHandlerTest` sử dụng Mockito cho Database và Event Publisher, xác minh rằng toàn bộ luồng nghiệp vụ của tính năng hoạt động chính xác.
- **JaCoCo Coverage**: Tích hợp plugin JaCoCo Maven để tạo báo cáo HTML trực quan về mức độ bao phủ code (test coverage), đảm bảo core logic của domain vượt qua yêu cầu tối thiểu là 80% coverage.

---

## 🧪 Cách Kiểm thử Hệ thống

Bạn có thể xác minh tính ổn định và chức năng của `messaging-service` bằng các phương pháp sau:

### 1. Kiểm thử Tự động (Unit & Integration)
Các bài test này chạy ngay lập tức trên source code để chứng minh tính đúng đắn của logic nghiệp vụ.

**Sử dụng Command Line (Git Bash / Linux / macOS):**
```bash
# Từ thư mục gốc của project:
cd messaging-service
../mvnw clean test jacoco:report
```

**Sử dụng Command Line (Windows CMD / PowerShell):**
```cmd
cd messaging-service
..\mvnw.cmd clean test jacoco:report
```

**Xem Báo cáo Coverage:**
Sau khi chạy thành công, hãy mở file sau bằng bất kỳ trình duyệt web nào để xem biểu đồ bao phủ code một cách trực quan:
`messaging-service/target/site/jacoco/index.html`

### 2. Kiểm thử Thủ công (End-to-End)
Phương pháp này đảm bảo `messaging-service` tương tác chính xác với frontend và các microservice khác.

**Điều kiện tiên quyết:**
1. Khởi động hạ tầng cơ sở (MySQL, RabbitMQ) thông qua Docker Compose:
   ```bash
   docker-compose up -d
   ```
2. Khởi động `messaging-service`:
   ```bash
   cd messaging-service
   ../mvnw spring-boot:run
   ```
3. Khởi động `auth-service` và ứng dụng frontend của bạn.

**Các kịch bản kiểm thử qua Frontend:**
- **Gửi tin nhắn:** Gõ một tin nhắn. Bạn sẽ thấy trạng thái "Đang gửi..." trong giây lát (Optimistic UI) trước khi nó được chốt trên màn hình.
- **Sửa tin nhắn:** Sửa lại văn bản của tin nhắn. Tải lại (refresh) trình duyệt để đảm bảo văn bản mới đã được lưu vĩnh viễn vào database.
- **Xóa tin nhắn:** Xóa tin nhắn. Văn bản sẽ cập nhật thành *"Tin nhắn đã bị thu hồi"*. Tải lại trang để xác minh.

**Xác minh Outbox Pattern (RabbitMQ):**
1. Mở RabbitMQ Management UI tại `http://localhost:15672` (đăng nhập mặc định: `guest` / `guest`).
2. Chuyển đến tab **Queues**.
3. Gửi một tin nhắn trên ứng dụng frontend của bạn.
4. Theo dõi biểu đồ RabbitMQ—bạn sẽ thấy một biểu đồ nhích nhẹ lên báo hiệu `MessageEventPublisher` đã lấy sự kiện từ bảng `outbox_messages` của MySQL và phát sóng nó thành công lên hàng đợi.
