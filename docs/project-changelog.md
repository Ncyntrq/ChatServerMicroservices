# Nhật ký thay đổi — Chat Server Microservices

> Ghi lại tất cả thay đổi đáng kể về tính năng, fix, phiên bản, và các mốc cột phát triển.
> Format: [YYYY-MM-DD] · [Phiên bản / Giai đoạn] · Mô tả.

---

## [2026-06-24] P2.2 & P2.3 — Keycloak OIDC + Gateway Dual-Mode OAuth2

**Giai đoạn hoàn thành:**
- ✅ P2.2 — Keycloak Identity Provider setup
- ✅ P2.3 — Gateway OAuth2 Resource Server + dual-mode JWT decoder
- ⏸️ P2.4 — Deferred (downstream service decentralized RBAC)

**Chi tiết:**

### Keycloak Integration (P2.2)
- **Infra:** Thêm 2 dịch vụ Docker (`keycloak-db` MySQL + `keycloak` image:24.0).
- **Realm:** `chatserver` được import tự động từ `devops/keycloak/realm-export.json`.
  - Token endpoint: `POST /realms/chatserver/protocol/openid-connect/token`
  - Hỗ trợ `grant_type=password` (Resource Owner Credentials) cho desktop client.
  - Hỗ trợ `grant_type=authorization_code` + PKCE S256 cho future web/mobile.
- **Clients:** 
  - `chat-client` (public, PKCE S256 + ROPC/password grant) — cho frontend & desktop app.
  - _(chat-backend confidential client: hoãn cùng Part D — sẽ thêm lại với secret quản lý qua env/secret manager khi triển khai service-to-service auth.)_
- **Bảo mật realm (đã siết theo security review):**
  - `sslRequired: external` (HTTPS bắt buộc cho non-loopback; loopback + docker private IP vẫn HTTP OK cho dev).
  - `redirectUris` giới hạn `http://localhost:3000/*`, `http://localhost:8080/*` (bỏ wildcard `*`); `webOrigins: "+"`.
  - ROPC (`directAccessGrantsEnabled`) giữ trên `chat-client` — hợp lý cho desktop client (không redirect-based); **phải tắt khi lên production web**.
- **Realm Roles:** `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SERVER_OWNER` seed vào db.
- **Dev User:** `testuser` / `test123` (ROLE_USER).
- **Token:** RS256 signature, 30 phút TTL (refresh token 7 ngày).
- **JWKS URI:** `http://keycloak:8080/realms/chatserver/protocol/openid-connect/certs`.
- **Env vars:** `KC_DB_PASSWORD`, `KC_ADMIN_PASSWORD`, `KEYCLOAK_ISSUER_URI`, `KEYCLOAK_JWKS_URI`.

### Gateway OAuth2 Resource Server (P2.3)
- **Spring Security WebFlux:** Gateway là OAuth2 Resource Server; verify JWT bắt buộc trên non-public routes.
- **DualModeReactiveJwtDecoder (Custom):**
  - Thử verify Keycloak token trước (RS256 via JWKS, lazy load).
  - Fallback → legacy HMAC HS256 từ `JWT_SECRET` (auth-service cũ).
  - Giữ tương thích token lâu dài trong quá độ.
- **Security Config:**
  - Permit: `/api/auth/**` (auth-service endpoints), `/ws/**` (WebSocket), `/actuator/**`.
  - Require JWT: tất cả path khác.
- **JwtAuthFilter (GlobalFilter):**
  - Inject downstream headers từ JWT claim:
    - `X-User-Id` = `preferred_username` (Keycloak) ∨ `sub` (legacy).
    - `X-Username` = `X-User-Id` (bảo đảm tương thích dữ liệu username-keyed hiện tại).
    - `X-User-Roles` = realm roles (comma-joined), rỗng nếu legacy token.
  - **Chiến lược username:** Không dùng ID số vì dữ liệu downstream hiện tại key theo username → phá dữ liệu + tình nạo migrate lớn → dành cho P2.4.
- **HMAC Note:** HS256 yêu cầu JWT_SECRET ≥ 32 ký tự; ≥ 64 ký tự → auth-service ký HS512 → cần update `MacAlgorithm.HS512` trong gateway.

### Breaking Changes
- Không có (token cũ vẫn hoạt động).

### Dependencies
- keycloak-db, keycloak phải healthy trước gateway.
- gateway xác thực ngoài Keycloak → bốc thêm network latency (mitigated: lazy JWKS load, ≤ 50ms per token).

### Documentation
- Cập nhật `docs/api-documentation.md` section 2 (xác thực):
  - Mô tả dual-mode + bảng so sánh.
  - Curl example lấy Keycloak token.
  - Header mới `X-User-Roles`.
- Cập nhật header reference (section 8.1).

### Future Work (P2.4 — Deferred)
- **Decentralized RBAC:** Downstream service tự verify JWT & quyền (không dùng X-User-Id cấp bởi gateway).
- **ID Migration:** username → numeric user ID (breaking change → cần DB backfill).
- **API Gateway Keycloak:** Gateway tái sử dụng Keycloak role (ROLE_ADMIN, ROLE_SERVER_OWNER) cho cơ chế phân quyền API.

---

## Tóm tắt phiên bản

| Giai đoạn | Trạng thái | Hoàn thành | Ngày |
|---|---|---|---|
| P1.x | ✅ Xong | 11/12 microservices + RabbitMQ + MinIO | ~2026-06-15 |
| P2.1 | ✅ Xong | Role-based access control (role-service) | 2026-06-24 |
| P2.2 | ✅ Xong | Keycloak OIDC setup | 2026-06-24 |
| P2.3 | ✅ Xong | Gateway OAuth2 Resource Server | 2026-06-24 |
| P2.4 | ⏸️ Deferred | Downstream RBAC + ID migration | TBD |
