# New Features — Discord-like Chat Client (CÒN LẠI)

> **Đã sàng lọc 2026-05-31.** Các mục #1–#6 trong bản kế hoạch gốc đã hoàn thành
> (đối chiếu với `changelog_summary.md` + code thực tế) và được loại bỏ khỏi tài liệu này.
>
> | Mục đã xong | File thực tế |
> |---|---|
> | Layout 3 cột | `gui/ChatClientGUI.java` |
> | DM & Friends sidebar | `gui/components/friends/FriendSidebar.java` |
> | Create/Join Server dialog | `gui/server/CreateServerDialog.java`, `JoinServerDialog.java` |
> | Invite link dialog | `gui/server/InviteCodeDialog.java` |
> | Create channel dialog | `gui/channel/CreateChannelDialog.java` |
> | User settings dialog | `gui/profile/UserSettingsDialog.java` |
>
> **Thứ tự triển khai còn lại: #7 → #8 → #9.**
>
> **Cập nhật 2026-05-31:** đã code xong #7, #8, #9 (xem ghi chú từng mục).
> ⚠️ Máy dev không có JDK/Maven → **chưa build/compile cục bộ**, cần build qua Docker/Jenkins.
>
> Ghi chú kiến trúc:
> - File đính kèm được mã hóa vào `content` của `MessageDTO` bằng marker (prefix `U+0001`),
>   nên backend lưu/broadcast/lịch sử hoạt động bình thường, **không cần đổi schema**.
> - Ghim tin nhắn hiện **lưu phía client theo phiên** của kênh đang mở (chưa có API ghim ở backend).
> - Ảnh/file tải qua gateway bằng `url?token=JWT` (gateway chấp nhận token query param).

---

## 7. Thanh công cụ nổi trên tin nhắn & Inline Edit (Message Hover Menu) — ✅ XONG

### Mô tả thiết kế UX

Khi di chuột hover qua một phần tử tin nhắn `ChatMessageItem`:

- Hiển thị một thanh công cụ nổi nhỏ nằm ở góc trên bên phải của khung tin nhắn đó.
- Nếu tin nhắn là của chính người dùng hiện tại: hiển thị nút "Sửa" (bút chì) và nút "Xóa" (thùng rác).
- Nếu là tin nhắn của người khác: chỉ hiển thị nút "Ghim" (Pin) và nút "Xóa" (nếu tài khoản hiện tại có quyền Admin/Moderator).

Khi người dùng click vào nút "Sửa" (hoặc nhấn phím Mũi tên lên ở ô chat để sửa tin nhắn cuối cùng của mình):

- Thay thế nhãn văn bản tin nhắn hiện tại bằng một ô nhập `JTextField` được viền màu xanh.
- Người dùng nhập nội dung mới, nhấn Enter để lưu thay đổi (gửi cập nhật qua WebSocket) hoặc nhấn Esc để hủy.

### Các file nguồn cần chỉnh sửa / triển khai

- Sửa đổi `client-app/src/main/java/gui/components/chat/ChatMessageItem.java` tích hợp Mouse Listener để ẩn/hiện toolbar nổi.
- Bổ sung cơ chế chuyển đổi Component hiển thị tin nhắn (Label ↔ Textfield) động trong dòng chat.
- Wire callback Sửa/Xóa/Ghim từ `ChatClientGUI` xuống `ChatMessageItem`, gửi lệnh qua `ChatWebSocketClient`.

---

## 8. Xem danh sách tin nhắn đã ghim (Pinned Messages Panel) — ✅ XONG

### Mô tả thiết kế UX

- Thanh Header của phòng chat trong Vùng 3 cần hiển thị biểu tượng chiếc Ghim 📌.
- Click vào đây sẽ kích hoạt hiển thị một Popup chứa danh sách các tin nhắn đã ghim trong kênh hiện tại.
- Mỗi tin nhắn hiển thị tên người gửi, avatar, ngày gửi, nội dung tin nhắn và nút "Bỏ ghim" (chỉ hiện đối với người dùng có quyền).

### Các file nguồn cần triển khai mới

- Tạo `client-app/src/main/java/gui/components/dialogs/PinnedMessagesDialog.java`.
- Sửa đổi `client-app/src/main/java/gui/ChatClientGUI.java` bổ sung thanh tiêu đề kênh chứa icon ghim.

---

## 9. Gửi file đính kèm & Preview hình ảnh trong chat — ✅ XONG

### Mô tả thiết kế UX

- Bên trái ô nhập liệu chat trong `ChatInputContainer` có biểu tượng chiếc kẹp giấy 📎 hoặc nút `+` lớn.
- Click vào đây kích hoạt `JFileChooser` để chọn tệp tin từ máy tính.
- Sau khi gửi:
  - **Nếu tệp tin là hình ảnh**: tạo một khung hình chữ nhật hiển thị ảnh thu nhỏ (thumbnail) trực tiếp trong danh sách tin nhắn chat. Khi click chuột vào ảnh sẽ mở cửa sổ phóng to ảnh gốc.
  - **Nếu là tệp tin văn bản khác (PDF, Zip, Docx)**: hiển thị một khối `JPanel` màu xám tối (File Card), vẽ icon loại tệp, ghi tên file, kích thước file và đính kèm nút "Tải xuống" dạng mũi tên chỉ xuống.

### Các file nguồn cần chỉnh sửa / triển khai

- Sửa đổi `client-app/src/main/java/gui/components/chat/ChatInputContainer.java` bổ sung nút đính kèm tệp tin (JFileChooser + upload qua file-service).
- Sửa đổi `client-app/src/main/java/gui/components/chat/ChatMessageItem.java` bổ sung logic vẽ tệp tin đính kèm (ảnh thumbnail hoặc card tải xuống).
