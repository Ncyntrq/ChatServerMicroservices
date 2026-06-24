# Triển Khai Keycloak & Hoàn Thành Phần Còn Lại Member 1

## Tình Trạng Hiện Tại — Đã Hoàn Thành

Sau khi khảo sát toàn bộ codebase, đây là trạng thái từng micro-phase:

| Phase | Tên | Trạng thái | Ghi chú |
|---|---|---|---|
| P0.1 | Quick-fix `create-drop` + dọn rác | ✅ Xong | `bin/` đã gitignore |
| P0.2 | Xóa hardcode secrets | ✅ Xong | `.env.example` có, JWT_SECRET không default |
| P1.1 | Dump schema + V1__init_schema.sql | ✅ Xong | `presence-service` đã có `V1__init_schema.sql` |
| P1.2 | Tích hợp Flyway | ✅ Xong | `presence-service` đã có flyway-core/mysql + config; notification/file đã `validate`+`flyway:true` |
| P1.3 | Health Check | ✅ Xong | Tất cả service có healthcheck trong docker-compose |
| P2.1 | Dockerfile Alpine + non-root | ✅ Xong | Tất cả 12 Dockerfile dùng `bellsoft/liberica-openjre-alpine:17` + `appuser` |
| P2.2 | Keycloak Setup | ✅ Xong | docker-compose (keycloak-db + keycloak:24), realm-export.json (chatserver), .env vars. Verify: realm load + token RS256 OK |
| P2.3 | Gateway OAuth2 Resource Server | ✅ Xong | Dual-mode `ReactiveJwtDecoder` (Keycloak JWKS RS256 → fallback HMAC HS256), `SecurityConfig` WebFlux, `JwtAuthFilter` bơm X-User-Id/Username/Roles. Build OK |
| P2.4 | RBAC phi tập trung | ⏸️ Hoãn | Quyết định: tạm bỏ. Gateway vẫn bơm header; downstream giữ nguyên (đọc `X-User-Id`). Làm khi cần |
| P3.1 | K8s Manifests | ✅ Xong | Đầy đủ base + overlays |
| P3.2 | HPA + Ingress | ✅ Xong | Có trong `overlays/prod/` |

## Tồn Đọng Cần Fix Nhanh

> [!WARNING]
> **notification-service** và **file-service** vẫn có `ddl-auto: update` (không phải `validate`) trong application.yml. Docker-compose đã set `SPRING_JPA_DDL_AUTO=validate` + `SPRING_FLYWAY_ENABLED=true` nên khi chạy Docker OK, nhưng default trong yml chưa thống nhất.

> [!WARNING]
> **presence-service** hoàn toàn thiếu Flyway — không có dependency trong pom.xml, không có config flyway trong application.yml, không có file `V1__init_schema.sql`.

---

## Proposed Changes

### Phần A: Fix Tồn Đọng (~30 phút)

#### [MODIFY] [application.yml](file:///Users/thanhnguyen/Documents/chat-server-microservices/notification-service/src/main/resources/application.yml)
- Đổi `ddl-auto: ${SPRING_JPA_DDL_AUTO:update}` → `${SPRING_JPA_DDL_AUTO:validate}`
- Đổi `flyway.enabled: ${SPRING_FLYWAY_ENABLED:false}` → `${SPRING_FLYWAY_ENABLED:true}`

#### [MODIFY] [application.yml](file:///Users/thanhnguyen/Documents/chat-server-microservices/file-service/src/main/resources/application.yml)
- Đổi `ddl-auto: ${SPRING_JPA_DDL_AUTO:update}` → `${SPRING_JPA_DDL_AUTO:validate}`
- Đổi `flyway.enabled: ${SPRING_FLYWAY_ENABLED:false}` → `${SPRING_FLYWAY_ENABLED:true}`

#### [MODIFY] [pom.xml](file:///Users/thanhnguyen/Documents/chat-server-microservices/presence-service/pom.xml)
- Thêm `flyway-core` + `flyway-mysql` dependency

#### [MODIFY] [application.yml](file:///Users/thanhnguyen/Documents/chat-server-microservices/presence-service/src/main/resources/application.yml)
- Thêm cấu hình flyway (enabled, baseline-on-migrate, baseline-version, locations)
- Đổi `ddl-auto: update` → `${SPRING_JPA_DDL_AUTO:validate}`

#### [NEW] V1__init_schema.sql cho presence-service
File: `presence-service/src/main/resources/db/migration/V1__init_schema.sql`
- Viết dựa trên entity JPA của presence-service

---

### Phần B: Keycloak Setup — P2.2 (~2 giờ code)

#### [MODIFY] [docker-compose.yml](file:///Users/thanhnguyen/Documents/chat-server-microservices/docker-compose.yml)
Thêm 2 service mới:

```yaml
keycloak-db:
  image: mysql:8.0
  container_name: keycloak-db
  environment:
    MYSQL_ROOT_PASSWORD: ${KC_DB_PASSWORD:-keycloak_pass}
    MYSQL_DATABASE: keycloak
  volumes:
    - keycloak-db-data:/var/lib/mysql
  healthcheck:
    test: ["CMD", "mysqladmin", "ping", "-h", "localhost"]
    interval: 10s
    timeout: 5s
    retries: 10
  networks:
    - chat-net

keycloak:
  image: quay.io/keycloak/keycloak:24.0
  container_name: keycloak
  command: start-dev --import-realm
  environment:
    KC_DB: mysql
    KC_DB_URL: jdbc:mysql://keycloak-db:3306/keycloak
    KC_DB_USERNAME: root
    KC_DB_PASSWORD: ${KC_DB_PASSWORD:-keycloak_pass}
    KEYCLOAK_ADMIN: admin
    KEYCLOAK_ADMIN_PASSWORD: ${KC_ADMIN_PASSWORD:-admin}
  ports:
    - "8180:8080"
  volumes:
    - ./devops/keycloak/realm-export.json:/opt/keycloak/data/import/realm-export.json
  depends_on:
    keycloak-db:
      condition: service_healthy
  networks:
    - chat-net
```

Thêm volume `keycloak-db-data` vào phần volumes.

#### [NEW] devops/keycloak/realm-export.json
Realm `chatserver` với:
- Client: `chat-client` (public, OIDC, PKCE)
- Client: `chat-backend` (confidential, service account cho backend)
- Realm roles: `ROLE_USER`, `ROLE_ADMIN`, `ROLE_SERVER_OWNER`
- Mappers: `sub` → userId, `realm_access.roles` → roles claim
- Default role: `ROLE_USER`

#### [NEW] devops/keycloak/migrate-users.sh
Script migration user từ `chat_auth_db` → Keycloak Admin REST API

#### [MODIFY] [.env.example](file:///Users/thanhnguyen/Documents/chat-server-microservices/.env.example)
Thêm biến Keycloak:
```env
KC_DB_PASSWORD=keycloak_pass
KC_ADMIN_PASSWORD=admin
KEYCLOAK_ISSUER_URI=http://keycloak:8080/realms/chatserver
```

#### [MODIFY] [.env](file:///Users/thanhnguyen/Documents/chat-server-microservices/.env)
Thêm biến Keycloak tương ứng

---

### Phần C: Gateway OAuth2 Resource Server — P2.3 (~2 giờ code)

#### [MODIFY] [pom.xml](file:///Users/thanhnguyen/Documents/chat-server-microservices/gateway-service/pom.xml)
- Thêm `spring-boot-starter-oauth2-resource-server`
- **Giữ** `jjwt-*` để hỗ trợ chế độ chuyển tiếp (dual auth)

#### [MODIFY] [application.yml](file:///Users/thanhnguyen/Documents/chat-server-microservices/gateway-service/src/main/resources/application.yml)
Thêm cấu hình OAuth2 Resource Server:
```yaml
spring:
  security:
    oauth2:
      resourceserver:
        jwt:
          issuer-uri: ${KEYCLOAK_ISSUER_URI:http://keycloak:8080/realms/chatserver}
          jwk-set-uri: ${KEYCLOAK_JWKS_URI:http://keycloak:8080/realms/chatserver/protocol/openid-connect/certs}
```

#### [NEW] SecurityConfig.java
File: `gateway-service/src/main/java/com/chatsever/gateway/config/SecurityConfig.java`
- `@EnableWebFluxSecurity`
- Permit: `/api/auth/**`, `/ws/**`, `/actuator/**`
- Authenticated: mọi request khác
- Keycloak JWT converter: extract `sub`, `realm_access.roles`

#### [MODIFY] [JwtAuthFilter.java](file:///Users/thanhnguyen/Documents/chat-server-microservices/gateway-service/src/main/java/com/chatsever/gateway/filter/JwtAuthFilter.java)
Chuyển thành **dual-mode filter** (giai đoạn chuyển tiếp):
- Thử verify bằng Keycloak JWKS trước
- Fallback verify bằng HMAC secret (cho token cũ từ auth-service)
- Inject headers: `X-User-Id`, `X-Username`, `X-User-Roles`

> [!IMPORTANT]
> **Chiến lược chuyển tiếp:** Giữ `auth-service` hoạt động song song. Token cũ (HMAC) vẫn được chấp nhận. Khi client chuyển hoàn toàn sang Keycloak → xóa fallback.

---

### Phần D: RBAC Phi Tập Trung cho Downstream — P2.4 (~1.5 giờ code)

#### [NEW] InternalAuthFilter.java
File: `common-lib/src/main/java/com/chatsever/common/security/InternalAuthFilter.java`
- Parse `X-User-Id`, `X-Username`, `X-User-Roles` từ header
- Set `SecurityContext` với `UsernamePasswordAuthenticationToken` chứa authorities

#### [NEW] InternalSecurityConfig.java
File: `common-lib/src/main/java/com/chatsever/common/security/InternalSecurityConfig.java`
- `SecurityFilterChain` chuẩn: CSRF off, stateless, actuator permitAll, others authenticated
- Auto-register `InternalAuthFilter`

#### Downstream services cần apply
Mỗi service chỉ cần thêm `@Import(InternalSecurityConfig.class)` hoặc tạo local `SecurityConfig` extends. Các service cần update:
- server-service, channel-service, messaging-service, presence-service, notification-service, file-service, role-service, user-profile-service, friend-service

> [!NOTE]
> `JwtClaimsExtractor` đã có sẵn trong common-lib. `InternalAuthFilter` sẽ sử dụng nó.

---

## Open Questions

> [!IMPORTANT]
> 1. **`auth-service` có giữ song song không?** Plan hiện tại giữ dual-mode (HMAC + Keycloak). Nếu muốn xóa hẳn auth-service → cần migration user trước.
> 2. **presence-service có entity JPA nào?** Cần xem entity để viết `V1__init_schema.sql`. Nếu presence chỉ lưu in-memory → không cần Flyway.

---

## Verification Plan

### Automated Tests
```bash
# 1. Build toàn bộ project
./mvnw clean package -DskipTests

# 2. Docker compose up
docker compose up -d keycloak-db keycloak
# Chờ Keycloak healthy, kiểm tra http://localhost:8180

# 3. Full system
docker compose up -d
```

### Manual Verification
- Truy cập `http://localhost:8180` → Admin Console → Realm `chatserver` exists
- Login bằng Keycloak → lấy token → gọi API qua gateway → verify 200 OK
- Gọi API bằng token cũ (HMAC) → vẫn hoạt động (dual mode)
- Kiểm tra downstream nhận `X-User-Id`, `X-User-Roles` header
