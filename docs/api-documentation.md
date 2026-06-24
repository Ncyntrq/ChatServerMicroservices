# Tài liệu API toàn diện — Chat Server Microservices

> Tài liệu mô tả **đầy đủ các kết nối API** của nền tảng chat realtime kiểu Discord.
> Bao gồm: REST API qua Gateway, WebSocket realtime, giao tiếp service-to-service (OpenFeign),
> và luồng sự kiện bất đồng bộ (RabbitMQ).

---

## Mục lục

- [1. Tổng quan kiến trúc giao tiếp](#1-tổng-quan-kiến-trúc-giao-tiếp)
- [2. Xác thực & Phân quyền](#2-xác-thực--phân-quyền)
- [3. API Gateway — Bảng định tuyến](#3-api-gateway--bảng-định-tuyến)
- [4. REST API theo từng service](#4-rest-api-theo-từng-service)
  - [4.1. auth-service](#41-auth-service-8081)
  - [4.2. user-profile-service](#42-user-profile-service-8090)
  - [4.3. server-service](#43-server-service-8085)
  - [4.4. channel-service](#44-channel-service-8086)
  - [4.5. role-service](#45-role-service-8091)
  - [4.6. messaging-service](#46-messaging-service-8082)
  - [4.7. presence-service](#47-presence-service-8083)
  - [4.8. friend-service](#48-friend-service-8092)
  - [4.9. notification-service](#49-notification-service-8088)
  - [4.10. file-service](#410-file-service-8089)
  - [4.11. log-service](#411-log-service-8084)
- [5. WebSocket Realtime](#5-websocket-realtime)
- [6. Giao tiếp service-to-service (OpenFeign)](#6-giao-tiếp-service-to-service-openfeign)
- [7. Luồng sự kiện bất đồng bộ (RabbitMQ)](#7-luồng-sự-kiện-bất-đồng-bộ-rabbitmq)
- [8. Quy ước chung & Mã trạng thái](#8-quy-ước-chung--mã-trạng-thái)

---

## 1. Tổng quan kiến trúc giao tiếp

Hệ thống dùng **3 kiểu kết nối API** kết hợp:

| Kiểu kết nối | Giao thức | Mục đích | Tính chất |
|---|---|---|---|
| **Client ↔ Backend** | REST/HTTP qua Gateway `:8080` | CRUD nghiệp vụ | Đồng bộ |
| **Client ↔ Messaging** | WebSocket `ws://…/ws/chat` | Chat realtime, typing, presence | Đồng bộ, hai chiều |
| **Service ↔ Service** | OpenFeign (REST nội bộ) | Truy vấn dữ liệu chéo service | Đồng bộ |
| **Service ↔ Service** | RabbitMQ Topic Exchange | Sự kiện presence / notify / log | Bất đồng bộ |

```
                    ┌──────────────┐
   Desktop Client → │   Gateway    │  Spring Cloud Gateway + JwtAuthFilter
   (Swing+FlatLaf)  │   :8080      │  Xác thực JWT → inject X-User-Id → định tuyến
                    └──────┬───────┘
        ┌──────────────────┼──────────────── REST (đồng bộ) ─────────────┐
   ┌────▼────┐  ┌─────────┐  ┌─────────┐  ┌──────────┐  ┌─────────┐
   │  auth   │  │ server  │  │ channel │  │   role   │  │ profile │  …
   └─────────┘  └────┬────┘  └─────────┘  └──────────┘  └─────────┘
                     │ OpenFeign (@FeignClient)
        ┌────────────┴───── Events (RabbitMQ chat.exchange) ─────────────┐
        ▼  presence.status / notify.# / log.#                            ▼
   messaging ──WebSocket──► Client                          notification / log
```

**Cổng dịch vụ:**

| Service | Cổng | Database |
|---|---|---|
| gateway-service | 8080 | — |
| auth-service | 8081 | `chat_auth_db` |
| messaging-service | 8082 | `chat_messaging_db` |
| presence-service | 8083 | `chat_presence_db` |
| log-service | 8084 | (file) |
| server-service | 8085 | `chat_server_db` |
| channel-service | 8086 | `chat_channel_db` |
| notification-service | 8088 | `chat_notification_db` |
| file-service | 8089 | `chat_file_db` |
| user-profile-service | 8090 | `chat_profile_db` |
| role-service | 8091 | `chat_role_db` |
| friend-service | 8092 | `chat_friend_db` |

**Hạ tầng:** MySQL `:3307` · RabbitMQ `:5672` (UI `:15672`) · MinIO `:9000` (Console `:9001`).

---

## 2. Xác thực & Phân quyền

### 2.1. Chế độ xác thực kép (Dual-Mode) — P2.2 & P2.3

Hệ thống hỗ trợ **hai loại JWT** trong giai đoạn chuyển tiếp khỏi auth-service tự quản:

| Loại token | Nguồn | Thuật toán | TTL | Đặc điểm |
|---|---|---|---|---|
| **Token Keycloak (mới)** | Keycloak OIDC provider | RS256 (RSA) | 30 phút | OpenID Connect standard, roles realm, JWKS public key |
| **Token legacy (cũ)** | auth-service | HS256 (HMAC) | 2h access / 7d refresh | Tương thích quá độ, fallback khi Keycloak unavailable |

**Luồng xác thực:**
1. Client gọi Keycloak token endpoint hoặc auth-service legacy endpoint (tùy chọn).
2. Mọi request gửi kèm header: `Authorization: Bearer <token>`.
3. **Gateway** (`JwtAuthFilter`, `DualModeReactiveJwtDecoder`) xác thực token:
   - Thử verify với Keycloak JWKS trước (RS256, nạp từ `jwk-set-uri` lazy).
   - Nếu thất bại hoặc không phải JWT Keycloak → fallback HMAC HS256 với `JWT_SECRET` dùng chung.
4. Gateway **inject** header cho downstream:
   - `X-User-Id` = `preferred_username` (Keycloak) hoặc `sub` (legacy token = username).
   - `X-Username` = giống `X-User-Id` (giữ ngữ cảnh username vì dữ liệu downstream dùng username-key).
   - `X-User-Roles` = realm roles từ Keycloak; rỗng với token cũ.
5. Các service phía sau **chỉ đọc header** để xác định người dùng (tin tưởng Gateway, không tự verify JWT).

> **Quan trọng:** Các service nghiệp vụ KHÔNG nhận token trực tiếp — chúng dựa vào `X-User-Id`, `X-Username`, `X-User-Roles` do Gateway gắn. Gọi thẳng vào service (bỏ qua Gateway) sẽ không có ngữ cảnh người dùng.

**Lưu ý:** JWT_SECRET cần ≥ 32 ký tự (256-bit) để hỗ trợ HS256 HMAC. Nếu dài ≥ 64 ký tự, auth-service sẽ ký HS512 → cần cập nhật `MacAlgorithm.HS512` trong gateway.

### 2.2. Lấy token Keycloak (OIDC)

**Token endpoint:** `POST http://localhost:8180/realms/chatserver/protocol/openid-connect/token`
(hoặc `http://keycloak:8080/realms/chatserver/...` từ nội bộ docker).

**Grant type: Password (Resource Owner Password Credentials)**
- Gửi username + password, nhận access token ngay (không qua browser).
- Public client `chat-client`: không cần secret.

```bash
curl -X POST http://localhost:8180/realms/chatserver/protocol/openid-connect/token \
  -H "Content-Type: application/x-www-form-urlencoded" \
  -d "grant_type=password" \
  -d "client_id=chat-client" \
  -d "username=testuser" \
  -d "password=test123"
```

**Response:**
```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cC...",
  "expires_in": 1800,
  "refresh_expires_in": 604800,
  "refresh_token": "eyJhbGciOiJIUzI1NiIsInR5cC...",
  "token_type": "Bearer"
}
```

Sử dụng `access_token` trong header: `Authorization: Bearer <access_token>`.

**Grant type: Authorization Code (OAuth2 Authorization Flow)**
- Cho frontend SPA: chuyển hướng browser tới `/oauth/authorize` → callback với code → trao code lấy token (ẩn secret với PKCE).
- Client `chat-client` đã bật `directAccessGrantsEnabled: true` (password grant) và PKCE S256.

### 2.3. Endpoint công khai (bỏ qua xác thực)

- `/api/auth/**` — đăng ký, đăng nhập (auth-service), refresh, validate.
- `/ws/**` — WebSocket (xác thực token ở messaging-service).
- `/actuator/**` — health checks, metrics.

### 2.4. Xác thực WebSocket

- Token truyền qua **query param**: `ws://localhost:8080/ws/chat?token=<jwt>`.
- Handler validate token (gọi auth-service hoặc giải mã nội bộ) rồi lưu `username` vào session.

### 2.5. Phân quyền RBAC (theo server)

- Vai trò định nghĩa theo từng server (`role-service`), dùng **permission bitmask** (Owner/Admin/Moderator…).
- Realm roles từ Keycloak (ROLE_USER, ROLE_ADMIN, ROLE_SERVER_OWNER) được gửi qua header `X-User-Roles` → có thể dùng để kiểm tra quyền hệ thống.
- Service kiểm tra quyền riêng per server qua FeignClient: `GET /api/servers/{serverId}/permissions/{userId}`.
- Hỗ trợ thao tác **kick** & **ban** thành viên.

---

## 3. API Gateway — Bảng định tuyến

Tất cả request từ client đi qua Gateway `:8080`. Bảng định tuyến `path prefix → service`:

| Path Pattern | Service đích | Cổng | Ghi chú |
|---|---|---|---|
| `/api/auth/**` | auth-service | 8081 | **Công khai** (không cần JWT) |
| `/api/users/**` | user-profile-service | 8090 | Hồ sơ & tìm kiếm người dùng |
| `/api/servers/**/roles/**`, `/permissions/**`, `/kick/**`, `/ban/**` | role-service | 8091 | Phân quyền, kick, ban |
| `/api/servers/**` (còn lại) | server-service | 8085 | Server, thành viên, mã mời |
| `/api/channels/{id}/messages/**` | messaging-service | 8082 | Lấy tin nhắn kênh |
| `/api/channels/*/ack/**` | notification-service | 8088 | Đánh dấu đã đọc |
| `/api/channels/**` (còn lại) | channel-service | 8086 | CRUD kênh, ghim tin |
| `/api/messages/**` | messaging-service | 8082 | DM & reaction |
| `/ws/**` | messaging-service | 8082 | WebSocket |
| `/api/presence/**` | presence-service | 8083 | Trạng thái hiện diện |
| `/api/notifications/**` | notification-service | 8088 | Thông báo |
| `/api/files/**` | file-service | 8089 | Upload/download tệp |
| `/api/friends/**` | friend-service | 8092 | Bạn bè |
| `/api/logs/**` | log-service | 8084 | Lịch sử log |

> Thứ tự khớp route quan trọng: các path con cụ thể (`roles`, `messages`, `ack`) phải được định tuyến **trước** route chung (`/api/servers/**`, `/api/channels/**`).

---

## 4. REST API theo từng service

> **Quy ước:** Tất cả path dưới đây là path **client gọi qua Gateway** (`http://localhost:8080`).
> Cột "Auth" cho biết header `X-User-Id` có bắt buộc không. Endpoint đánh dấu *(nội bộ)* chủ yếu phục vụ giao tiếp service-to-service.

### 4.1. auth-service (8081)

Base path: `/api/auth` · **Công khai**

| Method | Path | Request Body | Response | Auth | Mô tả |
|---|---|---|---|---|---|
| POST | `/api/auth/register` | `AuthRequest{username, password}` | `String` (message) | — | Đăng ký tài khoản mới |
| POST | `/api/auth/login` | `AuthRequest{username, password}` | `AuthResponse{accessToken, refreshToken, username}` | — | Đăng nhập, cấp token |
| POST | `/api/auth/validate` | `{token}` | `AuthResponse` | — | Kiểm tra tính hợp lệ token |
| POST | `/api/auth/refresh` | `{refreshToken}` | `AuthResponse` | — | Cấp lại access token |
| POST | `/api/auth/change-password` | `ChangePasswordRequest` | `{message}` | ✅ | Đổi mật khẩu |

### 4.2. user-profile-service (8090)

Base path: `/api/users`

| Method | Path | Request Body / Params | Response | Auth | Mô tả |
|---|---|---|---|---|---|
| GET | `/api/users/{username}/profile` | path: `username` | `UserProfile` | — | Lấy hồ sơ người dùng |
| PUT | `/api/users/profile` | `{displayName, bio, avatarUrl}` | `UserProfile` | ✅ | Cập nhật hồ sơ |
| POST | `/api/users/avatar` | multipart: `file` (≤ 2MB) | `UserProfile` | ✅ | Tải ảnh đại diện |
| PUT | `/api/users/status` | `{status}` | `UserProfile` | ✅ | Cập nhật custom status |
| GET | `/api/users/search` | query: `q` | `List<UserProfile>` | optional | Tìm người dùng (trừ chính mình) |

### 4.3. server-service (8085)

Base path: `/api/servers`

| Method | Path | Request Body / Params | Response | Auth | Mô tả |
|---|---|---|---|---|---|
| POST | `/api/servers` | `Server` | `Server` | ✅ | Tạo server mới |
| GET | `/api/servers` | query: `page`, `size`(=20) | `Page<Server>` | ✅ | Danh sách server của người dùng |
| GET | `/api/servers/{id}` | path: `id` | `Map<String,Object>` | — | Chi tiết server |
| PUT | `/api/servers/{id}` | `Server` | `Server` | ✅ | Cập nhật server (tên, icon…) |
| DELETE | `/api/servers/{id}` | path: `id` | `{message}` | ✅ | Xóa server (chỉ owner) |
| POST | `/api/servers/{id}/join` | query: `code` | `Map<String,Object>` | ✅ | Tham gia server theo mã mời |
| POST | `/api/servers/join` | query: `code` | `Map<String,Object>` | ✅ | Tham gia (tự tìm server theo mã) |
| POST | `/api/servers/{id}/leave` | path: `id` | `{message}` | ✅ | Rời server |
| POST | `/api/servers/{id}/invite` | path: `id` | `{message}` (mã mời) | ✅ | Sinh mã mời |
| POST | `/api/servers/{id}/ensure-member` | query: `userId` | `{message}` | — | *(nội bộ)* Xác minh là thành viên |
| PUT | `/api/servers/{serverId}/members/{userId}/roles` | `{roleIds:[...]}` | `{message}` | — | *(nội bộ)* Cập nhật role thành viên |

### 4.4. channel-service (8086)

Base path: `/api/channels`

| Method | Path | Request Body / Params | Response | Auth | Mô tả |
|---|---|---|---|---|---|
| POST | `/api/channels` | `ChannelRequest{name, serverId, type, topic}` | `ChannelDto` | optional | Tạo kênh |
| GET | `/api/channels/server/{serverId}` | path: `serverId` | `List<ChannelDto>` | — | Lấy danh sách kênh của server |
| PUT | `/api/channels/{channelId}` | `ChannelRequest` | `ChannelDto` | optional | Cập nhật kênh (tên, topic, slowmode) |
| DELETE | `/api/channels/{id}` | path: `id` | — | optional | Xóa kênh |
| DELETE | `/api/channels/server/{serverId}` | path: `serverId` | — | — | *(nội bộ)* Xóa toàn bộ kênh của server |
| POST | `/api/channels/{channelId}/pins/{messageId}` | path | `PinnedMessage` | optional | Ghim tin nhắn |
| DELETE | `/api/channels/{channelId}/pins/{messageId}` | path | `{message}` | optional | Bỏ ghim |
| GET | `/api/channels/{channelId}/pins` | path: `channelId` | `List<PinnedMessage>` | — | Danh sách tin đã ghim |
| PUT | `/api/channels/{channelId}/pin` | path: `channelId` | `ChannelDto` | optional | Bật/tắt ghim kênh (sidebar) |

### 4.5. role-service (8091)

Base path: `/api/servers`

| Method | Path | Request Body / Params | Response | Auth | Mô tả |
|---|---|---|---|---|---|
| POST | `/api/servers/{serverId}/roles` | `{roleName, color, permissions}` | `Role` | — | Tạo vai trò |
| GET | `/api/servers/{serverId}/roles` | path: `serverId` | `List<Role>` | — | Danh sách vai trò |
| PUT | `/api/servers/roles/{roleId}` | `{roleName, color, permissions}` | `Role` | — | Cập nhật vai trò |
| DELETE | `/api/servers/roles/{roleId}` | path: `roleId` | `{message}` | — | Xóa vai trò |
| PUT | `/api/servers/{serverId}/members/{userId}/roles` | `{roleIds:[...]}` | `Map<String,Object>` | — | Gán vai trò cho thành viên |
| GET | `/api/servers/{serverId}/permissions/{userId}` | path | `Map<String,Object>` | — | *(nội bộ)* Lấy quyền hiệu lực của user |
| POST | `/api/servers/{serverId}/kick/{userId}` | path | `{message}` | ✅ | Kick thành viên (cần quyền) |
| POST | `/api/servers/{serverId}/ban/{userId}` | `{reason?}` | `{message}` | ✅ | Ban thành viên (cần quyền) |
| GET | `/api/servers/{serverId}/ban/{userId}/check` | path | `Map<String,Object>` | — | *(nội bộ)* Kiểm tra user bị ban |
| POST | `/api/servers/{serverId}/roles/init` | path: `serverId` | `{message}` | — | *(nội bộ)* Khởi tạo vai trò mặc định |

### 4.6. messaging-service (8082)

Lấy/tìm tin nhắn qua REST; gửi tin realtime qua WebSocket (xem [mục 5](#5-websocket-realtime)).

| Method | Path | Request Body / Params | Response | Auth | Mô tả |
|---|---|---|---|---|---|
| GET | `/api/channels/{channelId}/messages` | query: `before`, `limit`(=50) | `List<ChatMessage>` | — | Lấy tin nhắn kênh (phân trang) |
| GET | `/api/channels/bulk` | query: `ids[]` | `List<ChatMessage>` | — | Lấy nhiều tin nhắn theo ID |
| GET | `/api/channels/search` | query: `channelId, serverId, keyword, limit` | `List<ChatMessage>` | — | Tìm tin nhắn theo từ khóa |
| GET | `/api/messages/private` | query: `targetUser, before, limit` | `List<ChatMessage>` | ✅ | Lấy hội thoại DM với 1 người |
| POST | `/api/messages/{messageId}/reactions/{emoji}` | path | — | optional | Thả cảm xúc |
| DELETE | `/api/messages/{messageId}/reactions/{emoji}` | path | — | optional | Gỡ cảm xúc |
| GET | `/api/messages/search` | query: `q`(≥2), `scope`(channel\|private\|all), `channelId`, `targetUser`, `limit` | `List<ChatMessage>` | ✅ | Tìm kiếm nâng cao |

### 4.7. presence-service (8083)

Base path: `/api/presence`

| Method | Path | Request Body / Params | Response | Auth | Mô tả |
|---|---|---|---|---|---|
| POST | `/api/presence/connect` | query: `username?` | `Map<String,String>` | header hoặc query | Đánh dấu online |
| POST | `/api/presence/disconnect` | query: `username?` | `Map<String,String>` | header hoặc query | Đánh dấu offline |
| GET | `/api/presence/online` | — | `List<String>` | — | Danh sách user online |
| GET | `/api/presence/all-statuses` | — | `Map<String,String>` | — | Trạng thái tất cả user |
| GET | `/api/presence/status/{userId}` | path: `userId` | `UserStatus` | — | Trạng thái 1 user |
| PUT | `/api/presence/status` | query: `status` | `Map<String,String>` | ✅ | Cập nhật trạng thái tùy chỉnh |

**Giá trị trạng thái:** `ONLINE`, `IDLE`, `AWAY`, `DO_NOT_DISTURB`, `INVISIBLE`, `OFFLINE`.

### 4.8. friend-service (8092)

Base path: `/api/friends` · Tất cả yêu cầu `X-User-Id`

| Method | Path | Request Body | Response | Mô tả |
|---|---|---|---|---|
| GET | `/api/friends` | — | `List<String>` | Danh sách bạn bè (đã chấp nhận) |
| GET | `/api/friends/pending` | — | `List<String>` | Lời mời kết bạn đang chờ |
| POST | `/api/friends/request` | `{targetUsername}` | `{message}` | Gửi lời mời kết bạn |
| POST | `/api/friends/accept` | `{targetUsername}` | `{message}` | Chấp nhận lời mời |
| POST | `/api/friends/reject` | `{targetUsername}` | `{message}` | Từ chối / hủy kết bạn |

> Service này gọi user-profile-service (RestTemplate) để xác minh user đích tồn tại.

### 4.9. notification-service (8088)

Base path: `/api` (path hỗn hợp)

| Method | Path | Request Body / Params | Response | Mô tả |
|---|---|---|---|---|
| GET | `/api/notifications` | query: `userId`, `unreadOnly` | `List<NotificationDTO>` | Lấy thông báo |
| GET | `/api/notifications/unread-count` | query: `userId` | `UnreadCountResponse` | Số tin chưa đọc theo kênh |
| POST | `/api/channels/{channelId}/ack` | `AckRequest` | — | Đánh dấu kênh đã đọc |
| PUT | `/api/notifications/{id}/read` | path: `id` | — | Đánh dấu 1 thông báo đã đọc |
| POST | `/api/notifications/ack-channel/{channelId}` | query: `userId` | — | *(nội bộ)* Ack chưa đọc theo kênh |
| POST | `/api/notifications/ack-dm/{senderUsername}` | query: `userId` | — | *(nội bộ)* Ack chưa đọc DM |

### 4.10. file-service (8089)

Base path: `/api/files`

| Method | Path | Params | Response | Mô tả |
|---|---|---|---|---|
| POST | `/api/files/upload` | multipart: `file` (≤ 10MB), `userId`, `channelId?` | `FileMetadataDTO` | Upload tệp |
| GET | `/api/files/{fileId}` | path: `fileId` | file stream | Tải tệp gốc |
| GET | `/api/files/{fileId}/thumbnail` | path: `fileId` | `image/jpeg` | Tải thumbnail |
| GET | `/api/files/{fileId}/info` | path: `fileId` | `FileMetadataDTO` | Metadata tệp |
| DELETE | `/api/files/{fileId}` | query: `userId` | — | Xóa tệp (chỉ owner) |
| GET | `/api/files/channel/{channelId}` | query: `type`(image\|document), `limit` | `List<FileMetadataDTO>` | Danh sách tệp theo kênh |

> Lưu trữ MinIO — bucket `chat-files` (tệp gốc) và `chat-thumbnails` (ảnh thu nhỏ tự sinh).

### 4.11. log-service (8084)

Base path: `/api/logs`

| Method | Path | Params | Response | Mô tả |
|---|---|---|---|---|
| GET | `/api/logs/history` | query: `page`(=0), `size`(=50, max 200), `eventType?` | `PagedResponse<LogEntry>` | Lịch sử hoạt động (phân trang) |

> Service này chủ yếu là **consumer RabbitMQ** (routing key `log.#`), endpoint REST chỉ để tra cứu lịch sử.

---

## 5. WebSocket Realtime

**Endpoint:** `ws://localhost:8080/ws/chat?token=<jwt>` (qua Gateway → messaging-service `:8082`).

**Luồng kết nối:**
1. Client kết nối kèm `?token=<jwt>`.
2. Handler validate token (gọi auth-service), lưu `username` vào session.
3. Thông báo presence-service (connect) → broadcast `JOIN` tới mọi client.
4. Khi đóng kết nối → thông báo presence-service (disconnect) → broadcast `LEAVE`.

**Các loại message (field `type` trong payload):**

| Type | Hướng | Mô tả |
|---|---|---|
| `CHAT` | Client → Server | Gửi tin nhắn vào kênh |
| `EDIT` | Client → Server | Sửa tin (chỉ chủ sở hữu) |
| `DELETE` | Client → Server | Xóa tin (chủ sở hữu hoặc admin) |
| `PRIVATE` | Client → Server | Gửi tin nhắn riêng (DM) |
| `TYPING` | Hai chiều | Báo đang gõ |
| `STATUS` | Server → Client | Đổi trạng thái user (từ presence) |
| `JOIN` | Server → Client | User vào (broadcast) |
| `LEAVE` | Server → Client | User rời (broadcast) |
| `PING` / `PONG` | Hai chiều | Heartbeat giữ kết nối |
| `ERROR` | Server → Client | Thông báo lỗi |

**Payload mẫu (`MessageDTO`):**
```json
{
  "type": "CHAT",
  "sender": "alice",
  "receiver": "bob",
  "channelId": 12,
  "serverId": 3,
  "content": "Xin chào!",
  "timestamp": "2026-06-16T13:00:00"
}
```

---

## 6. Giao tiếp service-to-service (OpenFeign)

Các service gọi nhau **đồng bộ** qua `@FeignClient` (REST nội bộ, dùng tên service trong Docker network).

| Service gọi | Service đích | Endpoint sử dụng | Mục đích |
|---|---|---|---|
| server-service | channel-service | `/api/channels/server/{serverId}`, … | Lấy/xóa kênh khi thao tác server |
| server-service | role-service | `/api/servers/{serverId}/roles/init`, `…/ban/{userId}/check`, `…/permissions/{userId}` | Khởi tạo role, kiểm tra ban/quyền |
| messaging-service | server-service | `/api/servers/{id}`, `/api/servers/{id}/ensure-member` | Xác minh server & tư cách thành viên |
| messaging-service | role-service | `/api/servers/{serverId}/permissions/{userId}` | Kiểm tra quyền (vd: xóa tin) |
| channel-service | role-service | `/api/servers/{serverId}/permissions/{userId}` | Kiểm tra quyền thao tác kênh |
| friend-service | user-profile-service | `/api/users/{username}/profile` (RestTemplate) | Xác minh user đích tồn tại |

---

## 7. Luồng sự kiện bất đồng bộ (RabbitMQ)

**Exchange chính:** `chat.exchange` (Topic, durable) · **Fanout:** `chat.fanout` (broadcast).

| Producer | Exchange | Routing Key | Queue | Consumer | Hành động |
|---|---|---|---|---|---|
| messaging-service (WS) | `chat.fanout` | — (fanout) | broadcastQueue | messaging-service | Broadcast tới mọi WS client |
| presence-service | `chat.exchange` | `presence.status` | presenceQueue | messaging-service | Broadcast trạng thái tới WS client |
| messaging-service | `chat.exchange` | `notify.message` | `chat.notification.queue` | notification-service | Tạo thông báo, đếm chưa đọc |
| messaging-service | `chat.exchange` | `log.{TYPE}` (vd `log.chat`) | `chat.log.queue` | log-service | Lưu log hoạt động |

**Sự kiện tiêu biểu** dùng chung cấu trúc `MessageDTO` (xem [mục 5](#5-websocket-realtime)).

**notification-service** phân tích `@mention` (`@username`, `@everyone`) và DM để tự sinh thông báo + đếm unread theo kênh/cuộc trò chuyện.

---

## 8. Quy ước chung & Mã trạng thái

### 8.1. Header chuẩn

| Header | Ai đặt | Ý nghĩa |
|---|---|---|
| `Authorization: Bearer <jwt>` | Client | Token xác thực (chỉ tới Gateway); hỗ trợ Keycloak RS256 & legacy HMAC HS256 |
| `X-User-Id` | Gateway | Username người dùng đã xác thực (từ token) |
| `X-Username` | Gateway | Username người dùng (giống X-User-Id, bảo đảm tương thích dữ liệu downstream) |
| `X-User-Roles` | Gateway | Danh sách realm roles từ Keycloak (comma-separated), rỗng với token legacy |
| `Content-Type: application/json` | Client | Cho hầu hết request body |
| `Content-Type: multipart/form-data` | Client | Cho upload tệp/avatar |

### 8.2. Mã trạng thái HTTP thường gặp

| Mã | Ý nghĩa |
|---|---|
| `200 OK` | Thành công |
| `201 Created` | Tạo tài nguyên thành công |
| `400 Bad Request` | Dữ liệu đầu vào sai |
| `401 Unauthorized` | Token thiếu/không hợp lệ |
| `403 Forbidden` | Không đủ quyền (RBAC) |
| `404 Not Found` | Không tìm thấy tài nguyên |
| `413 Payload Too Large` | Vượt giới hạn upload (tệp 10MB / avatar 2MB) |
| `500 Internal Server Error` | Lỗi phía server |

### 8.3. Actuator & Giám sát

Mỗi service phơi điểm cuối Spring Boot Actuator:
- `GET /actuator/health` — kiểm tra sức khỏe.
- `GET /actuator/prometheus` — metrics cho Prometheus (scrape 10s) → trực quan hóa qua Grafana.

---

> **Cập nhật cuối:** 2026-06-24 · Tài liệu này phản ánh trạng thái code hiện tại của 12 microservices.
> Khi thêm/sửa endpoint, vui lòng cập nhật bảng tương ứng để giữ tài liệu đồng bộ với mã nguồn.
> **P2.2–P2.3 (2026-06-24):** Thêm Keycloak OIDC + gateway dual-mode JWT decoder (RS256 + HMAC fallback).
