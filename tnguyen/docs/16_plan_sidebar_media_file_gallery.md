# Plan: Sidebar Lưu Trữ Ảnh/Video & File + Dropdown Thành Viên

> **Ngày tạo:** 2026-06-11  
> **Phạm vi:** Client-app GUI (Java Swing) + file-service backend (Spring Boot)

## Mô Tả Yêu Cầu

Cải tiến sidebar bên phải (EAST panel — `MemberListView`) khi đang ở trong 1 Server/Channel:

1. **Thêm section "Ảnh/Video"** — Hiển thị lưới ảnh/video nhỏ (thumbnail grid) đã được gửi trong channel hiện tại. Có nút "Xem tất cả" để mở gallery đầy đủ.
2. **Thêm section "File"** — Hiển thị danh sách file (tài liệu) đã gửi kèm tên, kích thước, ngày gửi. Có nút "Xem tất cả".
3. **Tất cả sections đều có tiêu đề dạng dropdown (collapsible)** — Bấm vào tiêu đề để mở/đóng nội dung bên trong:
   - **Thành viên** ▼ — Mặc định **đóng**, bấm vào mới hiện danh sách Trực tuyến/Ngoại tuyến
   - **Ảnh/Video** ▼ — Mặc định **mở**
   - **File** ▼ — Mặc định **mở**

Tham khảo giao diện mẫu (ảnh đính kèm): section "Ảnh/Video" hiển thị lưới 4 cột thumbnail, section "File" hiển thị danh sách card file.

---

## Kiến Trúc Hiện Tại

### Sidebar phải (EAST)
```
eastContainer (BorderLayout)
├── MemberListView (CENTER) — JScrollPane chứa listPanel (BoxLayout.Y)
│   ├── SidebarCategoryHeader("TRỰC TUYẾN — N")
│   │   └── UserListItem × N
│   └── SidebarCategoryHeader("NGOẠI TUYẾN — M")
│       └── UserListItem × M
└── MiniSidebar (EAST) — toggle hiện/ẩn
```

### File service backend
- **Repository:** `FileMetadataRepository` đã có `findByChannelId(Long channelId)`
- **Controller:** `FileController` chỉ có endpoint upload/download/info/delete, **chưa có** endpoint liệt kê files theo channel
- **DTO:** `FileMetadataDTO` có đầy đủ: `id, originalName, contentType, fileSize, url, thumbnailUrl, uploader, channelId, createdAt`

### Nguồn dữ liệu thay thế (client-side)
- Khi load lịch sử channel, client đã parse tất cả `ChatMessageItem` → nhận diện được attachment qua `ATTACH_PREFIX` marker
- Có thể **trích xuất ảnh/file từ lịch sử tin nhắn đã tải** mà không cần gọi thêm API backend

---

## Phân Tích 2 Phương Án Lấy Dữ Liệu

### Phương án A: Gọi API backend (file-service)
- **Ưu:** Lấy được TẤT CẢ file từng upload vào channel, không phụ thuộc vào limit tin nhắn  
- **Nhược:** Cần thêm endpoint backend (`GET /api/files/channel/{channelId}`)

### Phương án B: Trích xuất từ lịch sử tin nhắn đã tải (client-side)
- **Ưu:** Không cần sửa backend, triển khai nhanh  
- **Nhược:** Chỉ hiển thị file/ảnh trong phạm vi tin nhắn đã tải (thường là 1000 tin gần nhất)  

> **Đề xuất:** Dùng **Phương án A** (backend) để có dữ liệu đầy đủ. Phương án B làm fallback nếu API chưa sẵn sàng.

---

## Thiết Kế Chi Tiết

### 1. Component mới: `CollapsibleSection`

Thay thế `SidebarCategoryHeader` trong sidebar phải. Mỗi section có:
- **Tiêu đề** + icon mũi tên ▼/▶ (toggle)
- **Nội dung** (JPanel) — ẩn/hiện khi bấm tiêu đề
- Animation nhẹ (hoặc ẩn/hiện tức thì)

```
┌─────────────────────────────┐
│ ▼ Ảnh/Video                │  ← CollapsibleSection header
├─────────────────────────────┤
│ ┌────┬────┬────┬────┐      │  ← Grid thumbnail 4 cột
│ │ 📷 │ 📷 │ 📷 │ 📷 │      │
│ ├────┼────┼────┼────┤      │
│ │ 📷 │ 📷 │ 📷 │ 📷 │      │
│ └────┴────┴────┴────┘      │
│     [ Xem tất cả ]         │  ← Button
├─────────────────────────────┤
│ ▼ File                     │  ← CollapsibleSection header
├─────────────────────────────┤
│ ┌───────────────────────┐  │
│ │ 📄 report.pdf  2.1MB  │  │  ← FileCard item
│ │     ⏰ Hôm nay        │  │
│ ├───────────────────────┤  │
│ │ 📊 data.xlsx   500KB  │  │
│ │     ⏰ Hôm qua        │  │
│ └───────────────────────┘  │
│     [ Xem tất cả ]         │
├─────────────────────────────┤
│ ▶ Thành viên — 5          │  ← Mặc định đóng
└─────────────────────────────┘
```

### 2. Backend: Thêm endpoint liệt kê files theo channel

**File:** `file-service/.../controller/FileController.java`

```java
// 6. Liệt kê files theo channel (hỗ trợ phân loại ảnh/tài liệu)
@GetMapping("/channel/{channelId}")
public ResponseEntity<List<FileMetadataDTO>> getFilesByChannel(
        @PathVariable Long channelId,
        @RequestParam(value = "type", required = false) String type,  // "image" hoặc "document"
        @RequestParam(value = "limit", defaultValue = "20") int limit) {
    List<FileMetadataDTO> files = fileStorageService.getFilesByChannel(channelId, type, limit);
    return ResponseEntity.ok(files);
}
```

**File:** `file-service/.../service/FileStorageService.java`
- Thêm method `getFilesByChannel(channelId, type, limit)`
- Dùng `FileMetadataRepository.findByChannelId()` + filter theo `contentType` starts with `image/` hay không

**File:** `file-service/.../repository/FileMetadataRepository.java`
- Thêm query method:
```java
List<FileMetadata> findByChannelIdOrderByCreatedAtDesc(Long channelId);
List<FileMetadata> findByChannelIdAndContentTypeStartingWithOrderByCreatedAtDesc(Long channelId, String contentTypePrefix);
```

### 3. Client API: Thêm method vào `FileApiClient`

```java
/** Lấy danh sách files đã upload vào 1 channel */
public List<Map<String, Object>> getFilesByChannel(long channelId, String type, int limit) {
    // GET /api/files/channel/{channelId}?type=image&limit=8
}
```

### 4. Refactor `MemberListView` → `RightSidebarView`

Đổi tên + mở rộng sidebar phải để chứa 3 sections:

```
RightSidebarView extends JScrollPane
├── scrollPanel (BoxLayout.Y)
│   ├── CollapsibleSection("Ảnh/Video", expanded=true)
│   │   └── MediaGalleryPanel — lưới thumbnail + nút "Xem tất cả"
│   ├── CollapsibleSection("File", expanded=true)
│   │   └── FileListPanel — danh sách FileCard + nút "Xem tất cả"
│   └── CollapsibleSection("Thành viên — N", expanded=false)
│       └── memberListContent — danh sách UserListItem (logic cũ)
```

### 5. Sub-components mới

| Component | Mô tả |
|-----------|-------|
| `CollapsibleSection` | Header có mũi tên + panel nội dung ẩn/hiện |
| `MediaGalleryPanel` | Grid ảnh thumbnail (4 cột), load async, click = xem ảnh gốc |
| `FileListPanel` | Danh sách card file (icon badge + tên + size + ngày) |
| `MediaGalleryDialog` | Dialog "Xem tất cả" ảnh/video (full gallery) |
| `FileListDialog` | Dialog "Xem tất cả" file (full list) |

---

## Proposed Changes — Theo File

### Component 1: Backend — file-service

---

#### [MODIFY] [FileMetadataRepository.java](file:///Users/thanhnguyen/Documents/chat-server-microservices/file-service/src/main/java/com/chatsever/file/repository/FileMetadataRepository.java)
- Thêm 2 query methods: `findByChannelIdOrderByCreatedAtDesc`, `findByChannelIdAndContentTypeStartingWithOrderByCreatedAtDesc`

#### [MODIFY] [FileStorageService.java](file:///Users/thanhnguyen/Documents/chat-server-microservices/file-service/src/main/java/com/chatsever/file/service/FileStorageService.java)
- Thêm method `getFilesByChannel(channelId, type, limit)` — lọc và giới hạn kết quả

#### [MODIFY] [FileController.java](file:///Users/thanhnguyen/Documents/chat-server-microservices/file-service/src/main/java/com/chatsever/file/controller/FileController.java)
- Thêm endpoint `GET /api/files/channel/{channelId}` với query params `type` và `limit`

---

### Component 2: Client API — network

---

#### [MODIFY] [FileApiClient.java](file:///Users/thanhnguyen/Documents/chat-server-microservices/client-app/src/main/java/network/FileApiClient.java)
- Thêm method `getFilesByChannel(channelId, type, limit)` gọi `GET /api/files/channel/{channelId}`

---

### Component 3: Client GUI — Sidebar Components

---

#### [NEW] `gui/components/chat/CollapsibleSection.java`
- Header panel: tiêu đề + icon ▼/▶ + click listener toggle
- Content panel: ẩn/hiện con
- Style: font `CAPTION_BOLD`, color `TEXT_MUTED`, hover highlight

#### [NEW] `gui/components/chat/MediaGalleryPanel.java`
- Grid layout 4 cột thumbnail (~52×52 px mỗi ô)
- Load thumbnail async (`SwingWorker`)
- Tối đa 8 ảnh preview; nút "Xem tất cả" bên dưới
- Click thumbnail → xem ảnh gốc (reuse logic `openFullImage` từ `ChatMessageItem`)

#### [NEW] `gui/components/chat/FileListPanel.java`
- Danh sách tối đa 3 file cards
- Mỗi card: icon badge (PDF/DOC/XLS/ZIP), tên file, size, ngày tạo
- Nút "Xem tất cả" bên dưới
- Reuse `buildFileIconLabel` pattern từ `ChatMessageItem`

#### [NEW] `gui/components/dialogs/MediaGalleryDialog.java`
- Dialog modal hiển thị tất cả ảnh/video theo grid lớn hơn (có scroll)
- Click ảnh → zoom xem chi tiết

#### [NEW] `gui/components/dialogs/FileListDialog.java`
- Dialog modal hiển thị tất cả file với nút tải xuống

---

### Component 4: Client GUI — Sidebar chính

---

#### [MODIFY] [MemberListView.java](file:///Users/thanhnguyen/Documents/chat-server-microservices/client-app/src/main/java/gui/chat/MemberListView.java) → **RENAME** thành `RightSidebarView.java`
- Thêm `CollapsibleSection` cho Ảnh/Video, File, Thành viên
- Section "Thành viên" mặc định **đóng** (collapsed)
- Section "Ảnh/Video" và "File" mặc định **mở** (expanded)
- Thêm method `loadChannelMedia(channelId)` gọi API lấy ảnh/file
- Giữ nguyên logic `renderServerMembers()`, `renderOnline()`, `updateUserStatus()` trong section Thành viên

#### [MODIFY] [ChatClientGUI.java](file:///Users/thanhnguyen/Documents/chat-server-microservices/client-app/src/main/java/gui/ChatClientGUI.java)
- Đổi `MemberListView` → `RightSidebarView`
- Khi chọn channel (`onChannelSelected`): gọi `rightSidebar.loadChannelMedia(channelId)`
- Khi chuyển server/về Home: clear media sections

---

## Verification Plan

### Build & Compile
```bash
cd /Users/thanhnguyen/Documents/chat-server-microservices
./mvnw compile -pl file-service,client-app
```

### Manual Test
1. Mở app → vào 1 server → chọn 1 channel
2. Sidebar phải hiển thị 3 sections: Ảnh/Video (mở), File (mở), Thành viên (đóng)
3. Bấm tiêu đề "Thành viên" → mở ra danh sách online/offline
4. Bấm lại → đóng lại
5. Section Ảnh/Video hiển thị grid thumbnail từ channel
6. Bấm "Xem tất cả" → mở dialog gallery
7. Section File hiển thị 3 file gần nhất
8. Bấm "Xem tất cả" → mở dialog danh sách file đầy đủ

### Edge Cases
- Channel mới (chưa có file/ảnh) → hiển thị text "Chưa có ảnh/video" / "Chưa có file"
- Về Home (serverId == -1) → ẩn toàn bộ sidebar phải (giữ behavior hiện tại)
- DM → ẩn media sections (hoặc hiển thị nếu DM cũng có file — tuỳ quyết định)

---

## Open Questions

> [!IMPORTANT]
> **Q1:** DM (chat riêng) có cần hiển thị gallery ảnh/file không? Hiện tại sidebar phải bị ẩn khi ở DM.

> [!IMPORTANT]
> **Q2:** Nên **rename** `MemberListView` thành `RightSidebarView` luôn hay giữ tên cũ và thêm logic vào?
> - Rename → code sạch hơn, nhưng diff lớn
> - Giữ tên cũ → ít thay đổi, nhưng tên class không còn chính xác

> [!NOTE]
> **Q3:** Số lượng thumbnail preview mặc định là 8 (2 hàng × 4 cột). Có muốn nhiều hơn/ít hơn?

---

## Ước Lượng Thời Gian

| Phần | Thời gian |
|------|-----------|
| Backend endpoint | ~30 phút |
| Client API | ~15 phút |
| CollapsibleSection | ~30 phút |
| MediaGalleryPanel + Dialog | ~1 giờ |
| FileListPanel + Dialog | ~45 phút |
| Refactor MemberListView → RightSidebarView | ~1 giờ |
| Tích hợp vào ChatClientGUI | ~30 phút |
| Test & Polish | ~30 phút |
| **Tổng** | **~4.5 giờ** |
