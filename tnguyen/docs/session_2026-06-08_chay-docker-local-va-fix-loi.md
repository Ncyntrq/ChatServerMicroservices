# 🛠️ Nhật ký — Chạy backend Docker local & fix lỗi kết nối (2026-06-08)

Bối cảnh: chuyển từ test client trên backend remote (VM/VPS) sang **chạy backend bằng Docker local** để test client (chạy `gui.ClientApplication` trong IntelliJ). Ghi lại các lỗi gặp + cách sửa.

> Tóm tắt: 4 lỗi nối tiếp — (1) client còn trỏ remote, (2) thiếu `.env`, (3) MySQL OOM do Docker thiếu RAM, (4) JWT secret lệch gây "Lỗi từ server".

---

## 1. Client trỏ gateway remote → đổi sang localhost

**Triệu chứng:** client gọi backend trên VM/VPS thay vì máy local.

**Nguyên nhân:** `ApiConfig` mặc định `http://35.198.251.73:8080`.

**Sửa:** `client-app/.../network/ApiConfig.java` → mặc định `http://localhost:8080` + `ws://localhost:8080` (gateway docker map `8080:8080`). Vẫn override được khi cần test remote:
```
-Dchatsever.gateway.http=http://host:port -Dchatsever.gateway.ws=ws://host:port
```

---

## 2. Thiếu file `.env` cho docker-compose

**Triệu chứng:** `docker compose up` cảnh báo biến rỗng / service lỗi cấu hình.

**Nguyên nhân:** `.env` đã bị xóa khỏi repo (nằm trong `.gitignore`), compose cần `JWT_SECRET`, `MYSQL_*`, `RABBITMQ_*`, `MINIO_*`, `IMAGE_TAG`.

**Sửa:** tạo `.env` (giá trị dev, **không commit** — đã gitignore). Kiểm tra: `docker compose config` OK.

---

## 3. MySQL bị OOM-kill (Exited 137) → 500 register/login

**Triệu chứng:** register/login trả 500; auth-service log `UnknownHostException: mysql-db` / `Communications link failure`.

**Chẩn đoán:**
- `docker compose ps -a` → `mysql-db` **Exited (137)** = OOM-kill; các service phụ thuộc DB Exited(1) theo dây chuyền.
- `docker info` → Docker chỉ được cấp **1.9 GB RAM** → quá ít cho 15 JVM + MySQL + RabbitMQ + MinIO.

**Sửa (2 phần):**
1. **Tăng RAM Docker Desktop** → Settings → Resources → Memory ≥ 6 GB (đã tăng lên ~5.8 GB). Đây là fix bắt buộc.
2. **Thêm heap cap** cho 12 service trong `docker-compose.yml`: `JAVA_OPTS=-XX:+UseSerialGC -Xmx256m` (riêng `messaging-service` + `file-service` = `-Xmx512m` vì nặng hơn). Giảm RAM, tránh mỗi JVM tự lấy ~25% RAM rồi cộng dồn vỡ.

**Lưu ý:** Dockerfile chạy `java $JAVA_OPTS …` nên heap cap có hiệu lực ngay.
**Mẹo chạy nhẹ:** test login chỉ cần subset: `docker compose up -d --no-deps mysql-db auth-service gateway-service`.

---

## 4. "Lỗi từ server" trên API cần auth → JWT secret lệch

**Triệu chứng:** login OK nhưng các thao tác sau (load server/channel...) hay dính **"Lỗi từ server"**.

**Chẩn đoán:** gateway log `JWT signature does not match locally computed signature`.
- Cả `auth-service` và `gateway-service` đọc `secret: ${JWT_SECRET:chatsever-jwt-secret-key-2026-safe-key}`.
- Trong `docker-compose.yml` **chỉ `gateway` có `JWT_SECRET`** (từ `.env`); `auth-service` **không** → ký token bằng **default** baked-in.
- → Token ký bằng secret A, gateway validate bằng secret B → chữ ký sai → chặn request → client nhận body rỗng → `parseError()` trả chuỗi mặc định **"Lỗi từ server"**.

**Sửa:** thêm `JWT_SECRET=${JWT_SECRET}` vào `auth-service` trong `docker-compose.yml` → cả hai cùng secret từ `.env`. Recreate `auth-service` + `gateway-service`.

**Verify:** login lấy token (field JSON tên là **`token`**) → `GET /api/servers` trả **200**; gateway **0 SignatureException**.

**Cần làm sau fix:** đăng nhập lại (token cũ ký bằng secret default không còn hợp lệ).

> Chỉ `auth-service` + `gateway` dùng JWT; các service khác tin gateway qua header `X-User-Id`. Khi deploy remote phải set **cùng `JWT_SECRET`** cho cả auth + gateway.

---

## File đã đụng (config, không phải logic)
- `client-app/.../network/ApiConfig.java` — localhost mặc định
- `docker-compose.yml` — `JAVA_OPTS` (12 service) + `JWT_SECRET` cho `auth-service`
- `.env` — tạo mới (gitignored, không commit)

## Câu hỏi mở
- Khi deploy remote: quản lý `JWT_SECRET` qua secrets manager, đảm bảo auth + gateway luôn đồng bộ.
- Cân nhắc thêm `mem_limit` cho từng service trong compose để tránh OOM tái diễn trên máy RAM thấp.
