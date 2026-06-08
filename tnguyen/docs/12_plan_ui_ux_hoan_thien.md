# 📋 Plan UI/UX (v2) — Hoàn Thiện UX + Backend-Dependent + Refactor

> **Tiếp nối** [`11_plan_ui_ux.md`](./11_plan_ui_ux.md) (Phase 0→6 đã ✅ xong & merge `main`).
> **Nguyên tắc cũ:** chức năng + nút bấm trước → **giờ chuyển sang hoàn thiện & dọn dẹp**.
> **Trọng tâm v2:** (A) Hoàn thiện UX còn dở · (B) Tính năng cần backend · (C) Refactor code UI.
> **Vẫn giữ:** YAGNI · KISS · DRY · file < 200 dòng.

---

## Bối Cảnh — Đã Có Gì Trên `main`

Toàn bộ chức năng plan 11 + #7/#8/#9 (hover toolbar, pinned dialog, file attachment) + Friends/DM + Roles + Notifications + unread badges **đã merge main** (61 file Java client). Phần **chức năng coi như xong**, còn lại là **dọn dẹp UX, hoàn thiện tính năng dở, và nợ kỹ thuật**.

### Vấn đề tồn đọng (rà soát code thực tế 2026-06-08)

| # | Vấn đề | Bằng chứng (path:line) |
|---|--------|------------------------|
| 1 | `ChatClientGUI.java` **929 dòng** — 10 vùng trách nhiệm trộn lẫn, vi phạm rule <200 | `gui/ChatClientGUI.java` |
| 2 | **12+ "system message"** rác chèn vào luồng chat (đang tải lên, ghim, lỗi…) gây nhiễu | `ChatClientGUI.appendSystem` + các dòng 381,416,482,492,502,520,530,542,549,573,580,733,746,782 |
| 3 | Hover toolbar dùng `BorderLayout.EAST` + `setVisible` → **đẩy layout, không floating** | `ChatMessageItem.java:330,357,494` |
| 4 | File upload **chỉ check size 10MB**, không validate loại file, **không progress bar** | `ChatClientGUI.java:540`, `FileApiClient.java:31` |
| 5 | Nút Xóa **luôn enable** (server reject sau), context menu chỉ gate theo `isOwner` — **chưa role-based** | `ChatMessageItem.java:347`, `ChatClientGUI.java:843` |
| 6 | Ghim tin nhắn **chỉ lưu client/session** — F5 mất, người khác không thấy | `ChatMessage` entity chưa có field `pinned` |
| 7 | WS **broadcast toàn server + lọc client-side** theo `activeChannelId` | `ChatWebSocketClient` connect 1 lần, `ChatClientGUI.belongsToActiveChannel:660` |

### Phát hiện then chốt (giảm khối lượng việc)

✅ **Backend ĐÃ có API permission** — `GET /api/servers/{serverId}/permissions/{userId}` (`RoleController.java:92`) trả `permissionBitmask`. Client chỉ cần **gọi + đọc bit**, KHÔNG cần thêm backend cho Phase 2.

| Bit | Quyền | Giá trị |
|-----|-------|---------|
| `MANAGE_MESSAGES` | Xóa/ghim tin người khác | `4` |
| `MANAGE_CHANNEL` | Sửa/xóa channel | `8` |
| `KICK_MEMBER` | Kick | `16` |
| `ADMIN` | Toàn quyền | `128` |

---

## Bảng API Tham Chiếu (bổ sung v2)

| Service | Method | Endpoint | Trạng thái |
|---------|--------|----------|-----------|
| **Role** | GET | `/api/servers/{serverId}/permissions/{userId}` | ✅ Đã có |
| **Messaging** | POST | `/api/channels/{channelId}/messages/{messageId}/pin` | 🔜 Phase 5 (tạo mới) |
| **Messaging** | DELETE | `/api/channels/{channelId}/messages/{messageId}/pin` | 🔜 Phase 5 (tạo mới) |
| **Messaging** | GET | `/api/channels/{channelId}/pinned-messages` | 🔜 Phase 5 (tạo mới) |
| **Messaging (WS)** | — | `MessageType.SUBSCRIBE / UNSUBSCRIBE` | 🔜 Phase 6 (tạo mới) |

---

## Phase 0 — Refactor `ChatClientGUI` (Foundation)

> Tách "monster 929 dòng" thành controller nhỏ < 200 dòng **TRƯỚC**, để các phase sau thêm code vào file gọn thay vì phình thêm. Chỉ **di chuyển logic, không đổi hành vi**.

### Tạo mới (controller, mỗi file 1 trách nhiệm):
| File | Trách nhiệm | Gom từ method (dòng hiện tại) |
|------|-------------|------------------------------|
| `gui/chat/controller/ChatSessionController.java` | Init phiên + kết nối WS | `startSession:247`, `connectWebSocket:479` |
| `gui/chat/controller/IncomingMessageRouter.java` | Phân loại & xử lý tin WS đến | `handleIncoming:587`, `applyEdit:798`, `applyDelete:809`, `belongsToActiveChannel:660` |
| `gui/chat/controller/ChannelNavigationController.java` | Chuyển server/channel/DM + load history | `onServerSelected:322`, `onChannelSelected:351`, `switchToChannel:356`, `openDirectMessage:388` |
| `gui/chat/controller/MessageSendController.java` | Gửi chat/edit/delete | `sendChatFromInput:497`, `sendEdit:725`, `sendDelete:738`, `editLastOwnMessage:786` |
| `gui/chat/controller/PresenceController.java` | Member + online list + context menu | `loadPresence:422`, `loadServerMembersAndPresence:442`, `renderServerMembers:831`, `showMemberContextMenu:875`, `setOnlineUsers:910` |
| `gui/chat/controller/FileUploadController.java` | Chọn/validate/upload file | `chooseAndSendFile:527` (sẽ mở rộng Phase 3) |
| `gui/chat/controller/PinController.java` | Ghim/bỏ ghim/dialog | `openPinnedDialog:761`, `pinMessage:776`, `unpinMessage:767` (mở rộng Phase 5) |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/ChatClientGUI.java` | Chỉ giữ: **layout building** (constructor, `setChannelHeader`, ghép panel) + giữ tham chiếu tới các controller. Delegate toàn bộ logic. Mục tiêu **< 250 dòng** (chấp nhận hơi quá vì là orchestrator, nhưng tách rõ). |

### Nguyên tắc refactor:
- Controller nhận dependency qua constructor (chatHistoryPanel, wsClient, apiClients, callback) — **không tạo singleton mới**.
- Giữ nguyên signature callback đang dùng (`Consumer/BiConsumer/LongConsumer`) để sidebar không phải sửa.
- **Không đổi logic** — chỉ cut/paste + truyền tham chiếu. Build verify trước khi sang phase khác.

### Todo:
- [ ] Tạo 7 controller class, di chuyển method tương ứng
- [ ] `ChatClientGUI` chỉ còn layout + wiring controller
- [ ] Build `mvn -pl client-app package` sạch (verify trên máy có JDK/Docker)
- [ ] Smoke test: login → chọn server → channel → gửi tin → edit → delete → ghim → file (không regression)

### Success: build sạch, mọi hành vi giữ nguyên, mỗi controller < 200 dòng.

---

## Phase 1 — Toast/Snackbar (Thay "System Message" Nhiễu)

> Bỏ chèn thông báo tạm thời vào luồng chat. Thay bằng **toast nổi góc dưới-phải**, tự biến mất. Giữ lại system message THẬT (JOIN/LEAVE) trong luồng.

### Tạo mới:
| File | Mô tả |
|------|-------|
| `gui/components/feedback/ToastManager.java` | Singleton/holder hiển thị toast trên `JLayeredPane` của frame; queue + auto-dismiss (3s) |
| `gui/components/feedback/ToastItem.java` | 1 toast: icon (info/success/warn/error) + text + fade |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/ChatClientGUI.java` + controller | Thay các lời gọi `appendSystem(...)` mang tính **tạm thời/lỗi** → `ToastManager.show(level, msg)` |

### Phân loại 13 chỗ `appendSystem` (xem bảng vấn đề #2):
| Giữ trong luồng chat | Chuyển sang Toast |
|----------------------|-------------------|
| JOIN/LEAVE/SYSTEM thật từ server | "Đang tải lên…", "Đã ghim…", mọi "…thất bại", "vượt quá 10MB", "WS chưa sẵn sàng", "Thiếu JWT", lỗi load history |

### Luồng:
1. Hành động (gửi/upload/ghim/lỗi) → gọi `ToastManager.show(SUCCESS/ERROR/INFO, "…")`
2. Toast trượt lên góc dưới-phải, tự fade sau 3s; nhiều toast xếp chồng (stack)

### Todo:
- [ ] `ToastManager` + `ToastItem` trên `JLayeredPane` (không chặn input)
- [ ] Map 13 điểm `appendSystem` → giữ/chuyển theo bảng
- [ ] Verify: gửi lỗi → toast hiện, luồng chat **không còn rác**

### Success: luồng chat chỉ còn tin thật; feedback tạm hiện dạng toast tự biến mất.

---

## Phase 2 — Permission Gating Phía Client (Role-based UI)

> Dùng API permission CÓ SẴN để **ẩn/disable nút theo quyền thật** thay vì để server reject. Cache theo (serverId, userId).

### Tạo mới:
| File | Mô tả |
|------|-------|
| `network/PermissionApiClient.java` | `getPermissions(serverId, userId)` → đọc `permissionBitmask` (Map) |
| `network/PermissionCache.java` | Cache bitmask theo serverId; invalidate khi đổi server/role; helper `can(bit)` |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/chat/controller/ChannelNavigationController.java` | Khi chọn server → nạp permission của user hiện tại vào cache |
| `gui/components/chat/ChatMessageItem.java` | Nút 🗑 Xóa: chỉ enable nếu `isOwn` **hoặc** `can(MANAGE_MESSAGES)`. Nút 📌 Ghim: chỉ hiện nếu `can(MANAGE_MESSAGES)` |
| `gui/chat/controller/PresenceController.java` | "Cấp Role"/"Kick" trong context menu: gate theo `MANAGE_ROLES`/`KICK_MEMBER` thay vì chỉ `isOwner` |

### Helper bit (KISS):
```java
// PermissionCache
public boolean can(int bit) { return (bitmask & bit) != 0; }
static final int MANAGE_MESSAGES = 4, MANAGE_CHANNEL = 8, KICK_MEMBER = 16, MANAGE_ROLES = 64, ADMIN = 128;
```

### Todo:
- [ ] `PermissionApiClient` + `PermissionCache`
- [ ] Nạp permission khi đổi server (1 lần, cache)
- [ ] Gate nút Xóa/Ghim ở `ChatMessageItem` + context menu ở `PresenceController`
- [ ] Verify: user thường KHÔNG thấy nút ghim/xóa tin người khác; admin thì có

### Success: nút phản ánh đúng quyền thật, không còn "bấm rồi server reject".

---

## Phase 3 — File Upload UX (Validate Loại File + Progress)

> Bổ sung validate **content-type/extension** phía client + **progress bar** khi upload. Dùng toast (Phase 1) cho lỗi.

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/chat/controller/FileUploadController.java` | Trước upload: check extension trong whitelist (ảnh png/jpg/gif/webp, tài liệu pdf/docx/zip/txt…) + size 10MB (đã có). Sai loại → toast lỗi, dừng |
| `gui/components/chat/ChatInputContainer.java` | Hiển thị **progress bar/indeterminate** + nút Hủy khi đang upload; ẩn khi xong |
| `network/FileApiClient.java` | (Nếu khả thi) expose callback tiến độ; nếu HttpClient không hỗ trợ stream-progress dễ → dùng **indeterminate spinner** (KISS, YAGNI) |

### Quyết định kỹ thuật:
- Java `HttpClient` multipart không cho progress byte dễ → **mặc định indeterminate progress** (spinner + "Đang tải lên…") thay vì % chính xác. Tránh over-engineer.
- Whitelist extension: hằng số `Set<String>` trong `FileUploadController`.

### Todo:
- [ ] Whitelist extension + validate trước upload (toast nếu sai)
- [ ] Progress indeterminate + nút Hủy trên `ChatInputContainer`
- [ ] Verify: chọn `.exe` → bị chặn + toast; chọn ảnh lớn → thấy spinner → xong

### Success: chỉ loại file hợp lệ được gửi; có phản hồi tiến độ rõ ràng.

---

## Phase 4 — Hover Toolbar Overlay (JLayeredPane)

> Toolbar (✏/📌/🗑) hiện **floating đè lên** tin nhắn, KHÔNG đẩy layout. Mượt hơn `setVisible` trong BorderLayout hiện tại.

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/components/chat/ChatMessageItem.java` | Bỏ toolbar khỏi `BorderLayout.EAST`. Vẽ toolbar trên lớp overlay (dùng `OverlayLayout`/`JLayeredPane` cấp panel chat, hoặc đặt toolbar absolute ở góc trên-phải item). Hover → show, vị trí cố định, không reflow |

### Lưu ý:
- Kết hợp với Phase 2: toolbar chỉ render nút mà user **có quyền**.
- Giữ inline-edit (Label↔TextField) như cũ; chỉ đổi cách hiển thị toolbar.

### Todo:
- [ ] Chuyển toolbar sang overlay (không đẩy nội dung tin)
- [ ] Hover ổn định (không nhấp nháy khi di chuột qua nút)
- [ ] Verify: hover tin → toolbar nổi góc phải, layout đứng yên

### Success: toolbar nổi mượt, không xê dịch nội dung, đúng quyền.

---

## Phase 5 — Ghim Tin Nhắn Persist Backend (Full-stack)

> Hết "ghim chỉ ở client". Lưu `pinned` vào DB, có API, broadcast WS để mọi member thấy realtime.

### Backend — Sửa/Tạo:
| File | Thay đổi |
|------|----------|
| `messaging-service/.../entity/ChatMessage.java` | Thêm `Boolean isPinned` (default false); (optional) `pinnedBy`, `pinnedAt` |
| `common-lib/.../dto/MessageDTO.java` | Thêm `isPinned` (+ optional `pinnedBy/pinnedAt`) |
| `messaging-service` MessageType enum | Thêm `PIN` / `UNPIN` (broadcast realtime) |
| `messaging-service` Controller (`/api/channels/...`) | `POST .../{messageId}/pin`, `DELETE .../{messageId}/pin`, `GET .../{channelId}/pinned-messages` |
| `messaging-service` Service | `pin/unpin/listPinned`; check quyền qua `RoleClient` (reuse `canDeleteMessage`-style = `MANAGE_MESSAGES`) |
| WS handler `ChatWebSocketHandler.java` | Khi pin/unpin → broadcast `PIN/UNPIN` tới member server |

### Client — Sửa:
| File | Thay đổi |
|------|----------|
| `network/ChannelApiClient.java` (hoặc mới `PinApiClient`) | `pin(channelId,msgId)`, `unpin(...)`, `getPinned(channelId)` |
| `gui/chat/controller/PinController.java` | Gọi API thật thay vì lưu session; nhận `PIN/UNPIN` từ WS → cập nhật `PinnedMessagesDialog` realtime |
| `gui/components/dialogs/PinnedMessagesDialog.java` | Load từ `GET pinned-messages`; nút "Bỏ ghim" gọi DELETE; gate theo Phase 2 |

### Quyết định (trả lời câu hỏi mở):
- **Pin scope:** per-channel (đơn giản, đúng UX Discord).
- **Quyền pin:** dùng chung `MANAGE_MESSAGES (4)` — không tạo permission mới (YAGNI).
- **Phân trang:** offset/limit cơ bản (số pin nhỏ) — không cần cursor.

### Todo:
- [ ] Backend: field + DTO + enum + 3 endpoint + service + WS broadcast
- [ ] Client: API client + PinController nối API + dialog realtime
- [ ] Verify 2 máy: A ghim → B thấy ngay; F5 vẫn còn

### Success: ghim bền vững, đồng bộ realtime mọi member, gate đúng quyền.

---

## Phase 6 — WS Subscribe Theo Channel (Full-stack, Ưu tiên thấp)

> Giảm lọc client-side: client **subscribe đúng channel đang mở**, server chỉ đẩy tin liên quan. **YAGNI check:** broadcast+filter hiện vẫn chạy — chỉ làm nếu cần scale/giảm traffic. Để CUỐI.

### Backend — Sửa:
| File | Thay đổi |
|------|----------|
| `ChatWebSocketHandler.java` | `Map<username,session>` → thêm track `Map<username, Set<Long channelId>>`. Xử lý `SUBSCRIBE/UNSUBSCRIBE`. `broadcastToChannel` lọc thêm theo subscriber của channelId |
| MessageType enum | Thêm `SUBSCRIBE`, `UNSUBSCRIBE` |

### Client — Sửa:
| File | Thay đổi |
|------|----------|
| `network/ChatWebSocketClient.java` | Thêm `subscribe(channelId)`, `unsubscribe(channelId)` (gửi MessageDTO type SUBSCRIBE) |
| `gui/chat/controller/ChannelNavigationController.java` | Khi đổi channel: `unsubscribe(old)` + `subscribe(new)`; giảm/bỏ lọc `belongsToActiveChannel` |

### Todo:
- [ ] Backend track subscription + lọc broadcast
- [ ] Client subscribe/unsubscribe khi chuyển channel
- [ ] Verify: chỉ nhận tin của channel đang mở; chuyển channel nhận đúng

### Success: server chỉ đẩy tin channel đã subscribe; client gần như không phải lọc.

> ⚠️ **Cân nhắc YAGNI:** nếu quy mô nhỏ, có thể bỏ Phase 6. Ghi nhận như cải tiến scale tương lai.

---

## Thứ Tự Thực Hiện & Phụ Thuộc

| Phase | Nội dung | Phụ thuộc | Loại |
|-------|----------|-----------|------|
| **0** | Refactor ChatClientGUI → controllers | — | Refactor |
| **1** | Toast/Snackbar | 0 | UX |
| **2** | Permission gating client | 0 | UX/logic |
| **3** | File upload validate + progress | 0,1 | UX |
| **4** | Hover toolbar overlay | 0,2 | UX |
| **5** | Pin persist backend | 0,1,2 | Full-stack |
| **6** | WS subscribe per channel (optional) | 0 | Full-stack |

### File cần tạo mới (tổng hợp)
| # | File | Phase |
|---|------|-------|
| 1–7 | 7 controller trong `gui/chat/controller/` | 0 |
| 8 | `gui/components/feedback/ToastManager.java` | 1 |
| 9 | `gui/components/feedback/ToastItem.java` | 1 |
| 10 | `network/PermissionApiClient.java` | 2 |
| 11 | `network/PermissionCache.java` | 2 |
| 12 | `network/PinApiClient.java` (hoặc mở rộng ChannelApiClient) | 5 |

### File cần sửa (tổng hợp)
`ChatClientGUI.java` (0,1) · `ChatMessageItem.java` (2,4) · `ChatInputContainer.java` (3) · `PinnedMessagesDialog.java` (5) · `ChatWebSocketClient.java` (6) · `ChannelApiClient.java` (5) · backend: `ChatMessage.java`, `MessageDTO.java`, `ChatWebSocketHandler.java`, messaging controller/service, MessageType enum (5,6).

---

## Checklist Tổng Hợp

| # | Hạng mục | Phase |
|---|----------|-------|
| 1 | ChatClientGUI tách 7 controller, mỗi file <200 dòng | 0 |
| 2 | Toast thay 13 system-message rác | 1 |
| 3 | Nút Xóa/Ghim gate theo permission thật | 2 |
| 4 | Context menu Kick/Role gate theo bit | 2 |
| 5 | Validate loại file + chặn sai loại | 3 |
| 6 | Progress/spinner + Hủy khi upload | 3 |
| 7 | Toolbar hover floating (overlay) | 4 |
| 8 | Pin lưu DB + API + DTO field | 5 |
| 9 | Pin broadcast WS realtime | 5 |
| 10 | PinnedMessagesDialog dùng API thật | 5 |
| 11 | WS subscribe/unsubscribe per channel | 6 |

---

## Rủi Ro & Lưu Ý

- ⚠️ **Chưa build cục bộ** (máy dev không JDK/Maven, Docker nặng) → mọi phase **phải build verify qua Docker/Jenkins máy khác** trước khi sang phase tiếp.
- Phase 0 (refactor) **không đổi hành vi** — nếu lệch, regression toàn UI. Làm cẩn thận, commit nhỏ.
- Phase 5/6 đụng `common-lib` (MessageDTO) → ảnh hưởng nhiều service; thêm field **nullable/default** để backward-compatible.
- Tuân thủ: file <200 dòng, kebab-case path mới, không tạo file "enhanced" — sửa trực tiếp.

## Câu Hỏi Mở
1. Có cần `pinnedBy/pinnedAt` hiển thị trong dialog không, hay chỉ cần danh sách? (mặc định: thêm, rẻ).
2. Toast: vị trí góc dưới-phải có hợp với layout 3 cột hiện tại không, hay cần đặt trong vùng chat?

---

## Trạng Thái Thực Hiện (cập nhật 2026-06-08)

> **Phase 6 đã BỎ** theo yêu cầu. Build verify: `mvn -pl client-app -am -DskipTests package` → **BUILD SUCCESS** sau mỗi phase.

| Phase | Trạng thái | Ghi chú |
|-------|-----------|---------|
| **0 — Refactor** | ✅ Xong | `ChatClientGUI` 929→520 dòng (orchestrator) + tách 6 class `gui/chat/`: `ChatHistoryView`(139), `MemberListView`(~120), `UnreadCountSync`(99), `FileUploadController`(~95), `PinController`(58), `OutboundMessageController`(76). Hành vi giữ nguyên. |
| **1 — Toast** | ✅ Xong | `gui/components/feedback/Toast.java` (gộp manager+item theo KISS). ~13 system-message tạm/lỗi → toast nổi góc dưới-phải; chỉ SYSTEM thật từ server giữ trong luồng chat. |
| **2 — Permission** | ✅ Xong | `network/PermissionApiClient` + `PermissionCache` (singleton). Gọi `GET /api/servers/{id}/permissions/{userId}` khi đổi server. Gate nút Xóa/Ghim (`ChatMessageItem`) + context menu Kick/Role (`MemberListView`) theo bitmask. |
| **3 — File UX** | ✅ Xong | Whitelist extension + chặn loại tệp; `JProgressBar` indeterminate trong `ChatInputContainer` lúc upload. (Nút Hủy: bỏ — `HttpClient.send` blocking không hủy đáng tin, YAGNI.) |
| **4 — Hover toolbar** | ✅ Xong (pragmatic) | Đặt sẵn chiều rộng slot toolbar (right gutter) → hover không reflow text. Không dùng JLayeredPane (KISS/rủi-ro-thấp), vẫn đạt mục tiêu hết jitter. |
| **5 — Pin persist** | ⏸ Tạm dừng (theo yêu cầu) | Client trỏ gateway **remote** (`35.198.251.73:8080`); sửa backend (entity/DTO/API/WS) cần **deploy lại messaging-service + common-lib** mới test được. Làm sau khi sẵn sàng deploy. |

**Lệch so với plan (KISS/YAGNI):** Toast 1 file thay 2; thêm `OutboundMessageController` (ngoài 7 controller dự kiến); Phase 4 dùng gutter thay overlay; bỏ nút Hủy upload. ChatClientGUI còn 520 dòng (>200) — giữ vai orchestrator, ưu tiên readability theo rule.
