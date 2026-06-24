# Member 1 — Data & Infra Engineer · Tracking Plan

> Nguồn: `tnguyen/update/member1_plan.md` · Branch: `tnguyen-13/6` · Bắt đầu: 2026-06-22
> Quyết định phạm vi (user): Keycloak = **quyết định sau** · Kubernetes = **làm đầy đủ** · Nhịp độ = chạy P0+P1 liên tục rồi review.

## Trạng thái Micro-Phase

| Phase | Tên | Trạng thái | Test |
|---|---|---|---|
| P0.1 | Quick-fix `create-drop`→`update` + dọn `bin/` | ✅ Done | build xanh, compose config OK, git no `bin/` |
| P0.2 | Xóa hardcode secrets + `.env.example` + fail-fast | ✅ Done | grep no secret, compose config OK |
| P1.1 | Dump schema → `V1__init_schema.sql` (9 DB) | ✅ Done | 9 file, FK wrapper, header |
| P1.2 | Tích hợp Flyway + `ddl-auto: validate` | ✅ Done | auth+server: flyway_schema_history OK, validate pass |
| P1.3 | Health Check app services (compose) | ✅ Done | container thật: auth-service → **healthy**, flyway+validate OK |
| P2.1 | Dockerfile Alpine + non-root | ✅ Done | auth 314→190MB (-40%), file →203MB; non-root `appuser`; healthy |
| P2.2 | Keycloak Docker Compose + Realm | ❌ Bỏ (user chốt) | — |
| P2.3 | Keycloak Gateway OAuth2 | ❌ Bỏ (user chốt) | — |
| P2.4 | RBAC phi tập trung — `JwtClaimsExtractor` (common-lib) + WS verify cục bộ | ✅ Done | unit 4/4 pass, messaging boot OK (bỏ gọi auth-service/validate) |
| P3.1 | K8s Manifests + ConfigMap/Secrets | ✅ Done | `kubectl kustomize` dev=35 obj OK |
| P3.2 | K8s HPA + Ingress | ✅ Done | prod=38 obj OK, HPA messaging/gateway, Ingress chat.local |

## Ghi chú triển khai

- **presence-service**: không có JPA entity (stateless) → KHÔNG có Flyway/V1 schema. 9 service có schema, không phải 10.
- **Flyway strategy**: 7 service MySQL-always (auth, server, channel, messaging, user-profile, role, friend) → `ddl-auto: validate` + `flyway.enabled=true` mặc định. 2 service H2-dev (notification, file) → giữ H2 dev (`flyway.enabled=false`), bật qua env trong Docker (`SPRING_FLYWAY_ENABLED=true` + `SPRING_JPA_DDL_AUTO=validate`).
- **Schema FK**: dump bọc `SET FOREIGN_KEY_CHECKS=0/1` vì mysqldump xuất bảng theo alphabet (vd `member_role_ids` trước `members`).
- **Healthcheck**: dùng `wget` (có trong cả temurin-jammy lẫn alpine busybox) thay vì curl (alpine không có).
- **P2.1 base image**: plan ghi `eclipse-temurin:17-jre-alpine` nhưng image này **chỉ có amd64**, fail trên máy build arm64 (Apple Silicon). Đổi sang `bellsoft/liberica-openjre-alpine:17` (multi-arch arm64+amd64, JRE Alpine nhỏ, busybox `adduser`). `JAVA_OPTS` trong Dockerfile bị compose override (`-XX:+UseSerialGC -Xmx256m`) — giữ làm default khi chạy ngoài compose.

## P2.4 — Phạm vi RBAC (quan trọng)

- **Bỏ Keycloak** (user chốt). Phân quyền dựa trên JWT của auth-service hiện có.
- **Dữ liệu:** JWT chỉ chứa `sub`=username; bảng `users` không có cột role → **không có global role** để nhúng. Roles là **server-scoped** (permissionBitmask trong role-service), fetch per-request.
- **Đã làm (an toàn, đúng tinh thần "tự parse JWT, không gọi lại auth-service"):**
  - `common-lib/JwtClaimsExtractor`: `verifyAndGetSubject(token, secret)` (verify HMAC cục bộ) + helpers đọc header `X-User-Id`/`X-Username`/`X-User-Roles`.
  - `messaging-service` WebSocket: thay lời gọi mạng `auth-service/api/auth/validate` bằng verify JWT cục bộ → loại bỏ phụ thuộc tập trung.
- **KHÔNG áp** `anyRequest().authenticated()` SecurityFilterChain cho toàn bộ downstream: sẽ **chặn các lời gọi Feign nội bộ** (server→role, channel→role, messaging→role/server) vì chúng gọi trực tiếp (không qua gateway, không mang JWT). Đây là lỗ hổng thiết kế của plan gốc với codebase hiện tại. Muốn làm cần cơ chế service-to-service auth riêng → tách thành effort riêng.

## Checkpoint
- [x] P0 + P1 hoàn tất → báo cáo `implementation-260622-1416-GH-13-p0-p1-infra.md`.
- [x] Keycloak: user chốt **BỎ** (P2.2/P2.3).
- [x] P2.1 + P2.4 + P3.1 + P3.2 hoàn tất → báo cáo `implementation-260622-1512-GH-13-p2-p3-infra.md`.
- [x] Build toàn bộ xanh; mọi phase tự test.
- [ ] **Chưa commit** — chờ user duyệt (gợi ý tách commit theo từng phase).
