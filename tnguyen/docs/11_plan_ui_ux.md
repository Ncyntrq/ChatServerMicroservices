# 📋 Plan UI/UX — Client App (Java Swing)

> **Nguyên tắc:** Tạo đầy đủ **nút bấm + chức năng** trước, **thiết kế (polish)** sau.

---

## Tổng Quan Kiến Trúc Client Hiện Tại

```
client-app/src/main/java/
├── gui/
│   ├── ClientApplication.java          ← Entry point
│   ├── ChatClientGUI.java             ← Main chat window (hardcode serverId=1, channelId=1)
│   ├── auth/
│   │   ├── AuthDialog.java            ← Login/Register dialog (tabs)
│   │   ├── LoginPanel.java            ← Login form → mở ChatClientGUI
│   │   └── RegisterPanel.java         ← Register form
│   ├── components/
│   │   ├── navigation/
│   │   │   ├── ServerSidebar.java      ← Server icon bar (LEFT, 72px) — HARDCODED
│   │   │   └── ServerIconItem.java
│   │   ├── channels/
│   │   │   ├── ChannelSidebar.java     ← Channel list (240px) — HARDCODED
│   │   │   ├── ChannelListItem.java
│   │   │   └── UserFooterPanel.java    ← User footer (avatar + mic/headphone/⚙️)
│   │   ├── chat/
│   │   │   ├── ChatMessageItem.java
│   │   │   ├── ChatInputContainer.java
│   │   │   ├── UserListItem.java
│   │   │   ├── SidebarCategoryHeader.java
│   │   │   └── IconButton.java
│   │   ├── AvatarBadge.java
│   │   ├── AuthHeader.java
│   │   ├── FormField.java
│   │   └── PrimaryButton.java
│   └── theme/
│       ├── AppColors.java
│       └── AppFonts.java
└── network/
    ├── ApiConfig.java                  ← Gateway HTTP/WS URL + DEFAULT IDs
    ├── AuthApiClient.java              ← POST /api/auth/login, register, refresh
    ├── ChannelApiClient.java           ← GET /api/channels/{id}/messages
    ├── ChatWebSocketClient.java        ← WebSocket connection
    ├── SessionManager.java             ← JWT + username singleton
    ├── HttpClientHolder.java
    ├── JsonMapper.java
    ├── RawChatMessage.java
    └── ApiException.java
```

### Vấn Đề Hiện Tại
- **ServerSidebar** và **ChannelSidebar** đang **hardcode** dữ liệu (emoji icons, tên channel cố định)
- **ChatClientGUI** hardcode `activeServerId=1`, `activeChannelId=1` — không chuyển server/channel được
- **Không có** trang giới thiệu (landing page)
- **Không có** UI chỉnh sửa profile, tạo/sửa/xóa server, channel
- **Không có** mini sidebar hiển thị bạn bè online và servers đã join

---

## Bảng API Endpoint Tham Chiếu

| Service | Method | Endpoint | Mô tả |
|---------|--------|----------|-------|
| **Auth** | POST | `/api/auth/login` | Đăng nhập |
| **Auth** | POST | `/api/auth/register` | Đăng ký |
| **Auth** | POST | `/api/auth/change-password` | Đổi mật khẩu |
| **Profile** | GET | `/api/users/{username}/profile` | Xem hồ sơ |
| **Profile** | PUT | `/api/users/profile` | Cập nhật hồ sơ |
| **Profile** | POST | `/api/users/avatar` | Upload avatar |
| **Profile** | PUT | `/api/users/status` | Đặt trạng thái |
| **Profile** | GET | `/api/users/search?q=keyword` | Tìm kiếm user |
| **Server** | POST | `/api/servers` | Tạo server |
| **Server** | GET | `/api/servers` | Danh sách server đã join |
| **Server** | GET | `/api/servers/{id}` | Chi tiết server |
| **Server** | PUT | `/api/servers/{id}` | Cập nhật server |
| **Server** | DELETE | `/api/servers/{id}` | Xóa server |
| **Server** | POST | `/api/servers/{id}/join?code=xxx` | Tham gia server |
| **Server** | POST | `/api/servers/{id}/leave` | Rời server |
| **Server** | POST | `/api/servers/{id}/invite` | Tạo invite code |
| **Channel** | POST | `/api/channels` | Tạo channel |
| **Channel** | GET | `/api/channels/server/{serverId}` | DS channels của server |
| **Channel** | PUT | `/api/channels/{id}` | Cập nhật channel |
| **Channel** | DELETE | `/api/channels/{id}` | Xóa channel |
| **Presence** | GET | `/api/presence/online` | Danh sách user online |
| **Presence** | GET | `/api/presence/status/{userId}` | Trạng thái 1 user |

---

## Phase 0 — API Clients (Network Layer)

> Tạo REST client cho tất cả service cần gọi từ UI.

### Tạo mới:
| File | Mô tả |
|------|-------|
| `network/ServerApiClient.java` | CRUD server: create, getMyServers, getDetails, update, delete, join, leave, createInvite |
| `network/UserProfileApiClient.java` | CRUD profile: getProfile, updateProfile, uploadAvatar, updateStatus, searchUsers |
| `network/PresenceApiClient.java` | Presence: getOnlineUsers, getUserStatus |

### Sửa:
| File | Thay đổi |
|------|----------|
| `network/ChannelApiClient.java` | Thêm: `getChannelsByServer(serverId)`, `createChannel(request)`, `updateChannel(id, request)`, `deleteChannel(id)` |

### Chi tiết `ServerApiClient.java`:
```java
public class ServerApiClient {
    // SV1: POST /api/servers  {name, description, iconUrl}
    public Map<String,Object> createServer(String name, String description);
    
    // SV2: GET /api/servers  (X-User-Id header)
    public List<Map<String,Object>> getMyServers();
    
    // SV3: GET /api/servers/{serverId}
    public Map<String,Object> getServerDetails(long serverId);
    
    // SV4: PUT /api/servers/{serverId}  {name, description}
    public Map<String,Object> updateServer(long serverId, String name, String description);
    
    // SV5: DELETE /api/servers/{serverId}
    public void deleteServer(long serverId);
    
    // SV6: POST /api/servers/{serverId}/join?code=xxx
    public void joinServer(long serverId, String inviteCode);
    
    // SV7: POST /api/servers/{serverId}/leave
    public void leaveServer(long serverId);
    
    // SV8: POST /api/servers/{serverId}/invite
    public String createInviteCode(long serverId);
}
```

### Chi tiết `UserProfileApiClient.java`:
```java
public class UserProfileApiClient {
    // UP1: GET /api/users/{username}/profile
    public Map<String,Object> getProfile(String username);
    
    // UP2: PUT /api/users/profile  {displayName, bio}
    public Map<String,Object> updateProfile(String displayName, String bio);
    
    // UP4: PUT /api/users/status  {status}
    public Map<String,Object> updateStatus(String status);
    
    // UP5: GET /api/users/search?q=keyword
    public List<Map<String,Object>> searchUsers(String keyword);
}
```

---

## Phase 1 — User Profile CRUD (Chỉnh sửa thông tin cá nhân)

> Tab/Dialog để user xem + sửa profile (displayName, bio, avatar, status, đổi mật khẩu).

### Tạo mới:
| File | Mô tả |
|------|-------|
| `gui/profile/UserSettingsDialog.java` | JDialog modal — container chứa các tab settings |
| `gui/profile/ProfileEditPanel.java` | Panel chỉnh sửa: displayName, bio, avatar upload |
| `gui/profile/AccountSecurityPanel.java` | Panel đổi mật khẩu |
| `gui/profile/StatusPanel.java` | Panel đổi trạng thái (Online/Idle/DND/Invisible + custom text) |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/components/channels/UserFooterPanel.java` | Nút ⚙️ → mở `UserSettingsDialog` |

### Nút bấm cần tạo:
| Nút | Hành động | API |
|-----|-----------|-----|
| **"Lưu thay đổi"** (ProfileEditPanel) | PUT displayName + bio | `PUT /api/users/profile` |
| **"Upload Avatar"** (ProfileEditPanel) | Chọn file → upload | `POST /api/users/avatar` |
| **"Đổi mật khẩu"** (AccountSecurityPanel) | Nhập old/new pass → submit | `POST /api/auth/change-password` |
| **"Cập nhật trạng thái"** (StatusPanel) | Chọn status + text → submit | `PUT /api/users/status` |
| **"Đóng"** | Đóng dialog | — |

### Luồng:
1. User click ⚙️ ở UserFooterPanel
2. Mở `UserSettingsDialog` → load profile từ `GET /api/users/{username}/profile`
3. Hiển thị các tab: **Hồ sơ** | **Bảo mật** | **Trạng thái**
4. User sửa → bấm Lưu → gọi API tương ứng → hiện thông báo thành công/lỗi

---

## Phase 2 — Server CRUD (Tạo/Sửa/Xóa Server)

> Dialog quản lý server: tạo mới, sửa info, xóa, join bằng invite code, rời server.

### Tạo mới:
| File | Mô tả |
|------|-------|
| `gui/server/CreateServerDialog.java` | Dialog tạo server mới (nhập tên, mô tả) |
| `gui/server/EditServerDialog.java` | Dialog sửa server (đổi tên, mô tả) |
| `gui/server/JoinServerDialog.java` | Dialog join server bằng invite code |
| `gui/server/ServerSettingsDialog.java` | Dialog tổng hợp: xem info, sửa, xóa, tạo invite, rời |
| `gui/server/InviteCodeDialog.java` | Dialog hiển thị invite code đã tạo (copy-able) |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/components/navigation/ServerSidebar.java` | Nút ➕ → mở menu tạo/join server |
| `gui/components/navigation/ServerIconItem.java` | Right-click → context menu (Settings/Leave/Invite) |

### Nút bấm cần tạo:
| Nút | Hành động | API |
|-----|-----------|-----|
| **"Tạo Server"** (CreateServerDialog) | Nhập name, desc → POST | `POST /api/servers` |
| **"Lưu"** (EditServerDialog) | Sửa name/desc → PUT | `PUT /api/servers/{id}` |
| **"Xóa Server"** (ServerSettingsDialog) | Confirm → DELETE | `DELETE /api/servers/{id}` |
| **"Tham gia Server"** (JoinServerDialog) | Nhập code → POST | `POST /api/servers/{id}/join?code=xxx` |
| **"Rời Server"** (ServerSettingsDialog) | Confirm → POST | `POST /api/servers/{id}/leave` |
| **"Tạo Invite Code"** (ServerSettingsDialog) | POST → hiển thị code | `POST /api/servers/{id}/invite` |
| **"➕" button** (ServerSidebar) | Mở popup: Tạo Server / Join Server | — |

### Luồng tạo server:
1. Click ➕ trên ServerSidebar → popup 2 lựa chọn: "Tạo Server Mới" / "Tham Gia Server"
2. Nếu tạo mới → `CreateServerDialog` → nhập tên + mô tả → "Tạo Server" → `POST /api/servers`
3. Nếu join → `JoinServerDialog` → nhập invite code → "Tham gia" → `POST /api/servers/{id}/join?code=xxx`
4. Sau khi thành công → refresh ServerSidebar

### Luồng sửa/xóa server:
1. Right-click server icon → "Cài đặt Server"
2. Mở `ServerSettingsDialog` → load details từ `GET /api/servers/{id}`
3. Sửa tên/desc → "Lưu" | "Xóa Server" | "Tạo Invite" | "Rời Server"

---

## Phase 3 — Channel CRUD (Tạo/Sửa/Xóa Channel)

> Dialog quản lý channel trong server đang chọn.

### Tạo mới:
| File | Mô tả |
|------|-------|
| `gui/channel/CreateChannelDialog.java` | Dialog tạo channel (tên, loại TEXT/VOICE, category) |
| `gui/channel/EditChannelDialog.java` | Dialog sửa channel (tên, topic, slowmode) |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/components/channels/ChannelSidebar.java` | Thêm nút ➕ tạo channel; right-click channel → sửa/xóa |
| `gui/components/channels/ChannelListItem.java` | Right-click → context menu (Edit/Delete) |

### Nút bấm cần tạo:
| Nút | Hành động | API |
|-----|-----------|-----|
| **"➕" button** (ChannelSidebar header) | Mở CreateChannelDialog | — |
| **"Tạo Channel"** (CreateChannelDialog) | Nhập name, type → POST | `POST /api/channels` (body: `{serverId, name, type}`) |
| **"Lưu"** (EditChannelDialog) | Sửa name/topic → PUT | `PUT /api/channels/{id}` |
| **"Xóa Channel"** (context menu) | Confirm → DELETE | `DELETE /api/channels/{id}` |

### Luồng:
1. ChannelSidebar header có nút ➕ → mở `CreateChannelDialog`
2. Nhập tên, chọn loại (TEXT/VOICE) → "Tạo Channel" → `POST /api/channels`
3. Right-click 1 channel → "Sửa" → `EditChannelDialog` / "Xóa" → confirm → `DELETE`
4. Sau khi CRUD thành công → refresh ChannelSidebar

---

## Phase 4 — Main Sidebar (Server + Channel + Online Users)

> Tái cấu trúc ChatClientGUI để sidebar ĐỘNG, load dữ liệu từ API thay vì hardcode.

### Sửa lớn:
| File | Thay đổi |
|------|----------|
| `gui/ChatClientGUI.java` | Thêm ServerSidebar (WEST/LEFT) và ChannelSidebar (cạnh ServerSidebar). activeServerId và activeChannelId thành dynamic (có setter). Load danh sách server từ API khi mở app. Khi click server → load channels. Khi click channel → load messages + reconnect WS. Right sidebar hiển thị online members |
| `gui/components/navigation/ServerSidebar.java` | Load từ `GET /api/servers` → render danh sách icon. Click icon → callback chuyển server |
| `gui/components/channels/ChannelSidebar.java` | Load từ `GET /api/channels/server/{serverId}` → render danh sách. Click channel → callback chuyển channel |

### Callback flow:
```
ServerSidebar (click icon)
    → ChatClientGUI.onServerSelected(serverId)
        → ChannelSidebar.loadChannels(serverId)
            → Click channel → ChatClientGUI.onChannelSelected(channelId)
                → Load message history
                → Reconnect/re-subscribe WS cho channel mới
```

### Sidebar phải (online members):
- Load từ `GET /api/presence/online` + lọc theo server/channel
- Hiển thị nhóm: **ONLINE** / **OFFLINE** với status dot
- Cập nhật khi có tin `LIST` từ WebSocket

---

## Phase 5 — Landing Page (Trang Giới Thiệu)

> Trang intro hiển thị khi app mở, có nút Login/Register. Nếu đã có session → skip vô chat.

### Tạo mới:
| File | Mô tả |
|------|-------|
| `gui/landing/LandingFrame.java` | JFrame full — trang giới thiệu app |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/ClientApplication.java` | Kiểm tra SessionManager → nếu có token → vô ChatClientGUI luôn. Nếu không → hiển thị LandingFrame |

### Nút bấm cần tạo:
| Nút | Hành động |
|-----|-----------|
| **"Đăng nhập"** | Mở AuthDialog (tab Login) |
| **"Đăng ký"** | Mở AuthDialog (tab Register) |

### Luồng:
```
ClientApplication.main()
    ├── SessionManager.isAuthenticated() == true
    │   └── Validate token (POST /api/auth/validate)
    │       ├── Valid → Mở ChatClientGUI trực tiếp
    │       └── Invalid → clear session → hiện LandingFrame
    └── SessionManager.isAuthenticated() == false
        └── Hiện LandingFrame
            ├── Click "Đăng nhập" → AuthDialog (Login tab)
            └── Click "Đăng ký"  → AuthDialog (Register tab)
```

### Nội dung LandingFrame:
- Logo/tên app **"ChatSever"**
- Mô tả ngắn: "Nền tảng chat real-time theo phong cách Discord"
- 2 nút CTA: **Đăng nhập** | **Đăng ký**
- Footer: thông tin nhóm/version

---

## Phase 6 — Mini Sidebar (Bạn bè Online + Servers đã join)

> Thanh sidebar nhỏ/compact bên phải hoặc overlay, hiển thị bạn bè online và danh sách server user đang tham gia.

### Tạo mới:
| File | Mô tả |
|------|-------|
| `gui/components/mini/MiniSidebar.java` | Panel thu gọn (có thể toggle show/hide) |
| `gui/components/mini/FriendListPanel.java` | Danh sách bạn bè đang online (from Presence API) |
| `gui/components/mini/JoinedServerListPanel.java` | Danh sách server đã tham gia (from Server API) |

### Sửa:
| File | Thay đổi |
|------|----------|
| `gui/ChatClientGUI.java` | Thêm MiniSidebar vào layout (EAST hoặc toggle panel) |

### Dữ liệu:
| Panel | API | Refresh |
|-------|-----|---------|
| FriendListPanel | `GET /api/presence/online` | Mỗi 30s hoặc khi nhận WS event |
| JoinedServerListPanel | `GET /api/servers` | Khi mở sidebar / sau CRUD server |

### Nút bấm:
| Nút | Hành động |
|-----|-----------|
| **Toggle button** (trên toolbar) | Show/Hide MiniSidebar |
| **Click friend** | Mở DM (Direct Message) với user đó |
| **Click server** | Chuyển sang server đó (tương đương click ServerSidebar) |

---

## Thứ Tự Thực Hiện

| Phase | Nội dung | Dependencies |
|-------|----------|--------------|
| **Phase 0** | API Clients (network layer) | — |
| **Phase 1** | User Profile CRUD | Phase 0 |
| **Phase 2** | Server CRUD | Phase 0 |
| **Phase 3** | Channel CRUD | Phase 0 |
| **Phase 4** | Dynamic Sidebars (main refactor) | Phase 0, 2, 3 |
| **Phase 5** | Landing Page | — |
| **Phase 6** | Mini Sidebar | Phase 0, 4 |

### Tóm tắt file cần tạo mới (18 files):

| # | File | Phase |
|---|------|-------|
| 1 | `network/ServerApiClient.java` | 0 |
| 2 | `network/UserProfileApiClient.java` | 0 |
| 3 | `network/PresenceApiClient.java` | 0 |
| 4 | `gui/profile/UserSettingsDialog.java` | 1 |
| 5 | `gui/profile/ProfileEditPanel.java` | 1 |
| 6 | `gui/profile/AccountSecurityPanel.java` | 1 |
| 7 | `gui/profile/StatusPanel.java` | 1 |
| 8 | `gui/server/CreateServerDialog.java` | 2 |
| 9 | `gui/server/EditServerDialog.java` | 2 |
| 10 | `gui/server/JoinServerDialog.java` | 2 |
| 11 | `gui/server/ServerSettingsDialog.java` | 2 |
| 12 | `gui/server/InviteCodeDialog.java` | 2 |
| 13 | `gui/channel/CreateChannelDialog.java` | 3 |
| 14 | `gui/channel/EditChannelDialog.java` | 3 |
| 15 | `gui/landing/LandingFrame.java` | 5 |
| 16 | `gui/components/mini/MiniSidebar.java` | 6 |
| 17 | `gui/components/mini/FriendListPanel.java` | 6 |
| 18 | `gui/components/mini/JoinedServerListPanel.java` | 6 |

### Tóm tắt file cần sửa (8 files):

| # | File | Phase |
|---|------|-------|
| 1 | `network/ChannelApiClient.java` | 0 |
| 2 | `gui/components/channels/UserFooterPanel.java` | 1 |
| 3 | `gui/components/navigation/ServerSidebar.java` | 2, 4 |
| 4 | `gui/components/navigation/ServerIconItem.java` | 2 |
| 5 | `gui/components/channels/ChannelSidebar.java` | 3, 4 |
| 6 | `gui/components/channels/ChannelListItem.java` | 3 |
| 7 | `gui/ChatClientGUI.java` | 4, 6 |
| 8 | `gui/ClientApplication.java` | 5 |

---

## Checklist Nút Bấm Tổng Hợp

| # | Nút | Vị trí | Phase |
|---|-----|--------|-------|
| 1 | ⚙️ Settings (mở UserSettingsDialog) | UserFooterPanel | 1 |
| 2 | "Lưu thay đổi" profile | ProfileEditPanel | 1 |
| 3 | "Upload Avatar" | ProfileEditPanel | 1 |
| 4 | "Đổi mật khẩu" | AccountSecurityPanel | 1 |
| 5 | "Cập nhật trạng thái" | StatusPanel | 1 |
| 6 | ➕ "Tạo/Join Server" | ServerSidebar | 2 |
| 7 | "Tạo Server" | CreateServerDialog | 2 |
| 8 | "Tham gia Server" | JoinServerDialog | 2 |
| 9 | "Lưu" (server info) | EditServerDialog | 2 |
| 10 | "Xóa Server" | ServerSettingsDialog | 2 |
| 11 | "Rời Server" | ServerSettingsDialog | 2 |
| 12 | "Tạo Invite Code" | ServerSettingsDialog | 2 |
| 13 | ➕ "Tạo Channel" | ChannelSidebar header | 3 |
| 14 | "Tạo Channel" (submit) | CreateChannelDialog | 3 |
| 15 | "Lưu" (channel info) | EditChannelDialog | 3 |
| 16 | "Xóa Channel" (context menu) | ChannelListItem | 3 |
| 17 | Server icon click → select | ServerSidebar | 4 |
| 18 | Channel item click → select | ChannelSidebar | 4 |
| 19 | "Đăng nhập" | LandingFrame | 5 |
| 20 | "Đăng ký" | LandingFrame | 5 |
| 21 | Toggle MiniSidebar | ChatClientGUI toolbar | 6 |
| 22 | Click friend (open DM) | FriendListPanel | 6 |
| 23 | Click server (switch) | JoinedServerListPanel | 6 |

---

> **Trạng thái:** ✅ Hoàn thành Phase 0 → 6 — client-app build (`mvn package`) thành công.
>
> | Phase | Trạng thái |
> |-------|-----------|
> | 0 — API Clients | ✅ ServerApiClient, UserProfileApiClient, PresenceApiClient + ChannelApiClient CRUD; thêm `AuthApiClient.changePassword`, `FormField.setText` |
> | 1 — User Profile CRUD | ✅ 4 panel + ⚙️ UserFooterPanel mở UserSettingsDialog; đổi mật khẩu nối API thật |
> | 2 — Server CRUD | ✅ 5 dialog; ➕ ServerSidebar popup Tạo/Join; right-click icon → Settings |
> | 3 — Channel CRUD | ✅ Create/Edit dialog; ➕ header ChannelSidebar; right-click channel → Sửa/Xóa |
> | 4 — Dynamic Sidebars | ✅ ChatClientGUI ghép ServerSidebar(WEST)+ChannelSidebar; load động từ API; chọn server→channel→message; online từ Presence |
> | 5 — Landing Page | ✅ LandingFrame + ClientApplication check session; AuthDialog nhận owner |
> | 6 — Mini Sidebar | ✅ MiniSidebar + FriendListPanel + JoinedServerListPanel; toggle 👥 trên toolbar |
>
> **Ghi chú còn lại:** upload avatar (multipart) và DM vẫn là placeholder; WS lọc tin theo `channelId` phía client (chưa subscribe theo channel).
