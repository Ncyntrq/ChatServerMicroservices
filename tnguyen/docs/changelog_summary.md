# 🛠 Báo Cáo Thay Đổi & Nâng Cấp Hệ Thống (Changelog)

Tài liệu này tổng hợp toàn bộ các chỉnh sửa, nâng cấp, và các tính năng mới đã được triển khai trên cả **Client (UI)** và **Backend (Microservices)** để đáp ứng yêu cầu của hệ thống Chat thời gian thực.

---

## 1. Dịch Vụ Quản Lý Vai Trò (Role Service)
Đây là phần nhận được nhiều cập nhật nhất để giải quyết triệt để lỗi "null" và hoàn thiện quy trình phân quyền.

### 🟢 Thêm mới
- **API `PUT /api/servers/roles/{roleId}`**: Bổ sung Endpoint để cho phép chủ Server cập nhật lại Tên và Mã màu của Role.
- **API `DELETE /api/servers/roles/{roleId}`**: Bổ sung Endpoint để xóa một Role khỏi Server.
- **`RoleApiClient` (Client)**: Khai báo 2 phương thức `updateRole` và `deleteRole` để kết nối giao diện với Backend.
- **Bảng Màu Trực Quan (JColorChooser)**: Tích hợp bảng chọn màu hiển thị ngay trên giao diện tạo/sửa Role, loại bỏ việc nhập mã Hex thủ công.

### 🟡 Thay đổi (Cập nhật)
- **Giao diện `RoleManagementDialog`**: Đập đi xây lại hoàn toàn theo kiến trúc Bảng chia đôi (Split-Pane). Cột trái là danh sách, cột phải là Form chỉnh sửa luôn hiển thị.
- **Sửa lỗi hiển thị chữ `null`**: Đổi ánh xạ đọc JSON từ `r.get("name")` thành đúng chuẩn API trả về là `r.get("roleName")`. Giờ đây tên Role mới tạo được hiển thị chính xác ngay lập tức.
- **Độ rộng Textbox**: Canh lại kích thước ô nhập Tên Role, giới hạn chiều dài và đưa vào `FlowLayout` để không bị kéo giãn mất thẩm mỹ.
- **Chế độ Mặc định**: Vừa mở cửa sổ Quản lý Role là kích hoạt ngay chế độ "Tạo Role Mới" (luôn mở Form bên phải) để có thể gõ ngay không cần click thêm nút nào.

### 🔴 Xóa bỏ
- **Bỏ Logic tự sinh 4 Role mặc định**: Trong `ServerServiceImpl` của hệ thống Backend, xóa bỏ lời gọi API `initDefaultRoles`. Giờ đây khi tạo Server mới, danh sách Role sẽ hoàn toàn trống, không tự đẻ ra 4 Role rác nữa.
- **Dọn dẹp Database**: Xóa sạch toàn bộ các bản ghi bị lỗi `null` (thực chất là 4 Role cũ) trong Database `chat_role_db` thông qua lệnh SQL của Docker.

---

## 2. Hệ Thống Nhắn Tin (Messaging Service)
Xử lý các sự cố ngăn cản việc tải tin nhắn cũ.

### 🟡 Thay đổi (Cập nhật)
- **Cấu hình API Gateway**: Fix lỗi định tuyến khiến tính năng Chat Riêng trả về mã lỗi `500 (No static resource api/messages/private)`.
- **Khắc phục Crash ở `MessageDTO`**: Cập nhật lại logic Deserialize (giải mã) chuỗi JSON của `java.time.LocalDateTime`, giúp ứng dụng Client không bị văng lỗi khi cố gắng hiển thị lịch sử trò chuyện.

### 🔴 Xóa bỏ
- **Bỏ Log tải trò chuyện**: Xóa bỏ hoàn toàn các thông báo rác dạng "Đang tải trò chuyện...", "WebSocket mở kết nối..." trên luồng hiển thị tin nhắn ở giao diện Client, giúp trải nghiệm chat liền mạch và tinh gọn hơn.

---

## 3. Hệ Thống Server & Channel (Server / Channel Service)
Tinh chỉnh luồng trải nghiệm khi tạo và tham gia Server.

### 🟡 Thay đổi (Cập nhật)
- **Logic Tạo Kênh Mặc Định**:
  - Tại `ServerServiceImpl`, khi một Server mới sinh ra, hệ thống tự gọi ngầm sang `channel-service` để tạo kênh `# General`.
  - **Sửa Lỗi Phân Quyền (400 Bad Request)**: Thêm Header `X-User-Id` mang ID của chủ Server vào API nội bộ để `channel-service` xác thực quyền tạo kênh.
- **Giao diện Thanh Channel**:
  - Cập nhật hiển thị tên Kênh: Thêm tiền tố `#` (như Discord), đổi tên hiển thị từ "Kênh" thành "Kênh chat".
  - Chuyển biểu tượng dấu cộng `+` xuống dưới và sửa lỗi hiển thị ô vuông. Bấm vào nguyên dòng để tạo kênh mới.
- **Mã Mời (Invite Code)**: Cải tiến UI và Logic gia nhập Server, giờ chỉ yêu cầu bạn nhập Mã Mời (không cần ID server).

---

## 4. Dịch Vụ Hồ Sơ & File (Profile & File Upload)
Hỗ trợ thay đổi diện mạo cá nhân theo thời gian thực.

### 🟢 Thêm mới
- **API Tải File & Cập Nhật Hồ Sơ**: Gắn kết tính năng Upload ảnh (PNG, JPG) cho việc đổi Avatar của cá nhân và Avatar của Server.
- Cập nhật giao diện tự động Reload ảnh Avatar mới trên thanh điều hướng sau khi Upload thành công.

### 🟡 Thay đổi (Cập nhật)
- **UI Hồ Sơ**: Chuyển biểu tượng mở "Cài đặt hồ sơ" từ dấu ba chấm sang biểu tượng **Bánh Răng ⚙️**.

---

## 5. Trạng Thái và Bạn Bè (Presence / Friend Service)
Xử lý các yêu cầu cập nhật Real-time khắt khe.

### 🟡 Thay đổi (Cập nhật)
- **Màn hình Home Real-time**: Lời mời kết bạn giờ đây sẽ pop-up và hiển thị ngay lập tức nhờ bắt sự kiện qua mạng thay vì phải F5 hoặc chuyển màn hình.
- Trạng thái Online/Offline cũng được làm mới liên tục khi có user đăng nhập/đăng xuất.
- **Cấu hình Docker `friend-service`**: Sửa lỗi pull access denied cho `friend-service` bằng cách thiết lập tính năng tự động Build local (Build image trực tiếp trên máy) `docker compose up -d --build friend-service`.

### 🔴 Xóa bỏ
- **Thanh thành viên ở Home**: Bỏ hoàn toàn thanh Danh sách thành viên bên phải giao diện Home, vì bên trái đã có Danh sách Bạn bè, tránh gây trùng lặp và rối rắm giao diện.
- **Ẩn Khung Nhập Text**: Khi không ở trong 1 đoạn chat nào, vùng nhập tin nhắn sẽ tự động biến mất.
