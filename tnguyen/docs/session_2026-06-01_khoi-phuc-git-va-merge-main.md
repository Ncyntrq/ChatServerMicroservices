# 🛠️ Nhật ký — Khôi phục git hỏng & merge main (2026-06-01)

Branch: `tnguyen-1/6`. Bối cảnh: git của thư mục làm việc (`chat-server-microservices (local)`) bị hỏng; có thêm 1 bản clone mới từ `main`. Mục tiêu ban đầu: "merge 2 folder thủ công". **Kết quả thực tế: khôi phục được git nên không cần copy file — dùng `git merge` đúng chuẩn.**

> ⚠️ **Chưa build/compile cục bộ** (không có JDK/Maven). Mọi kiểm tra dưới đây là **rà soát tĩnh** (cân bằng ngoặc + đối chiếu symbol). Cần build qua Docker/Jenkins trên máy khác để verify runtime.

---

## 1. Chẩn đoán mức độ hỏng

| Thành phần | Trạng thái |
|---|---|
| `.git/HEAD` | ❌ hỏng — 28 byte null |
| `.git/refs/heads/tnguyen-1/6` | ❌ rỗng |
| `.git/logs/HEAD` (reflog) | ✅ nguyên — cho biết tip commit |
| `.git/ORIG_HEAD` | ✅ `0d3d0b6...` |
| `refs/heads/tnguyen-30/5`, `tnguyen-31/5` | ✅ `0d3d0b6...` |
| Toàn bộ git **objects** | ✅ nguyên vẹn |

➡️ Chỉ 2 file con trỏ bị ghi đè null. Loại hỏng **khôi phục được hoàn toàn**.

## 2. Khôi phục git (không phá dữ liệu)

- Backup file hỏng: `.git/HEAD.corrupt.bak`, `.git/tnguyen-1-6.corrupt.bak`.
- Ghi lại `HEAD` = `ref: refs/heads/tnguyen-1/6` và ref `tnguyen-1/6` = `0d3d0b6` (theo reflog/ORIG_HEAD).
- `git fsck --full`: không có object hỏng/thiếu (chỉ "dangling" — bình thường).
- Kết quả: repo đọc lại được, đủ lịch sử + 2 commit của mình (`b812106`, `0d3d0b6`).

## 3. Lưu công việc đang dở

- `git status` lộ ra **56 file chưa commit** (working tree): friend-service, private messaging, role management, nhiều UI component, docs phiên trước.
- Commit lại toàn bộ → `f2e5a80` (*"feat(client): tiếp tục hoàn thiện UI/UX + friend/private-message/role (plan 11)"*).
- Tạo nhánh bảo hiểm: **`backup/recovered-2026-06-01`** (giữ trạng thái trước merge).

## 4. Merge main mới

- Base nhánh: `f2cf38a` (main 30/5). Main đã tiến **25 commit** → `69b0d05` (PR #27–#30: update notification, updateUII).
- `git fetch origin main` + `git merge origin/main` → **15 file xung đột**.

## 5. Giải quyết 15 xung đột (chiến lược: KẾT HỢP, không bỏ bên nào)

| File | Cách xử lý |
|---|---|
| `Jenkinsfile` | Giữ biến `NEWMAN_TEST_DIR` (HEAD) |
| `docker-compose.yml` | Giữ port RabbitMQ `5672:5672` (main) |
| `network/FileApiClient.java` *(add/add)* | Giữ bản HEAD (superset: `uploadFile`, `download`, `isTrustedGatewayUrl`) |
| `friends/FriendSidebar.java` *(add/add)* | Lấy main: thêm `friendItems` + `updateUnreadCounts` (đếm tin chưa đọc) |
| `components/AvatarBadge.java` | Giữ HEAD: tải avatar qua `FileApiClient.download()` (chống lộ JWT tới host lạ) |
| `channels/UserFooterPanel.java` | Giữ HEAD: nút mic/deafen |
| `navigation/ServerIconItem.java` | Giữ HEAD: tải icon qua `download()` an toàn |
| `network/UserProfileApiClient.java` | **Lấy main**: `uploadAvatar` qua file-service/MinIO (nhất quán với `FileApiClient` + `AvatarBadge`). ⚠️ `buildAvatarMultipart` thành không dùng (chỉ warning) |
| `chat/ChatInputContainer.java` | Giữ HEAD: nút "+" nối `onAttach.run()` (đính kèm thật) |
| `chat/UserListItem.java` | Lấy main: `setUnreadCount` + badge tin chưa đọc (FriendSidebar gọi tới) |
| `chat/ChatMessageItem.java` | **Kết hợp**: giữ đính kèm/inline-edit/toolbar (HEAD) + thêm `isConsecutive`/`createMessageBodyWrapper` (main). Thêm constructor 5 tham số `(msg, highlighted, currentUser, actions, isConsecutive)` |
| `profile/ProfileEditPanel.java` | Giữ HEAD (biến `selected`, hiển thị `avatarUrl`) |
| `gui/ChatClientGUI.java` | **Kết hợp** (13 hunk): giữ fileApi + header kênh + ghim tin + sửa/xóa/ghim (HEAD) + thêm `notificationApi` + `lastSender` + `refreshUnreadCounts`/`ackMessage` + gộp tin liên tiếp (main). DM history: duyệt xuôi + lọc `[SYSTEM_FRIEND_UPDATE]` (main, đúng vì API đã tự `Collections.reverse`) |
| `server/.../ServerServiceImpl.java` | **Giữ HEAD**: tạo kênh "General" với `ownerId` (không dùng `"system"` của main) — an toàn quyền hơn |
| `profile/.../UserProfileService.java` | Giữ HEAD: `toSafeRelativeMediaUrl` (chỉ lưu path nội bộ, chống lộ token) |

## 6. Kiểm tra tĩnh

- `git diff --check`: không còn marker xung đột (chỉ trailing whitespace từ code main — vô hại).
- Quét toàn repo: **0 marker** `<<<<<<< / ======= / >>>>>>>` còn sót.
- **Cân bằng dấu ngoặc** toàn bộ file đã sửa → bắt được **1 lỗi thiếu `}`** trong `ChatMessageItem.createMessageBodyWrapper` (đã sửa). Sau sửa: tất cả cân bằng.
- Xác minh symbol nối giữa 2 nhánh đều tồn tại: `NotificationApiClient.{getUnreadCounts,ackDm,ackChannel}`, `ChannelSidebar.updateUnreadCounts`, `UnreadBadgePanel.setCount`, `IconButton(String)` + `IconButton(String,ActionListener)`, `MessageDTO.{getReceiver,setReceiver,getIsEdited,setServerId,...}`, `MessageActions.onEdit`. ✅

## 7. Hoàn tất

- Merge commit: **`aec3d7e`** (2 cha: `f2e5a80` công việc của mình + `69b0d05` main).
- `git merge-base --is-ancestor origin/main HEAD` → **YES** (main hợp nhất hoàn toàn).
- `git fsck --connectivity-only` → sạch. Working tree sạch.

## 8. ⚠️ Cần review / bước tiếp theo

1. **Build thật** qua Docker/Jenkins (chưa compile cục bộ).
2. Xem lại 2 lựa chọn ngữ nghĩa: (a) `UserProfileApiClient.uploadAvatar` dùng file-service; (b) `ServerServiceImpl` tạo kênh bằng `ownerId`.
3. Bản repo tốt nhất giờ là thư mục **`(local)`** (đủ lịch sử + remote). Folder clone mới có thể bỏ.
4. Khi build OK: `git push origin tnguyen-1/6` → mở PR. Có sự cố thì quay về nhánh `backup/recovered-2026-06-01`.

---

*Ghi chú: file `HEAD.corrupt.bak` và `tnguyen-1-6.corrupt.bak` nằm trong `.git/` — có thể xóa sau khi xác nhận mọi thứ ổn.*
