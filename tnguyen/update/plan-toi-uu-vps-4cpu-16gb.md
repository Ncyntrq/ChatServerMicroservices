# Plan Tối Ưu Hệ Thống Cho VPS 4 vCPU / 16 GB

> Mục tiêu: hết lag/giật, startup nhanh, deploy không fail — chạy ổn 13 microservices + hạ tầng trên 4 vCPU / 16 GB.
> Liên quan: [debug-260625-0024-tinh-trang-lag-he-thong.md](../../plans/reports/debug-260625-0024-tinh-trang-lag-he-thong.md)

## Chẩn đoán gốc (tóm tắt)

- **Nút thắt #1 = CPU**: cold-start 12 JVM đồng thời trên 4 vCPU → startup 187s, deploy fail.
- **Nút thắt #2 = RAM khi CI chạy chung**: SonarQube (~3.5GB) + Jenkins (~1.5GB) + observability (~1.4GB) + app (~7.3GB) ≈ **14GB/16GB** → sát trần, spike khi build → OOM risk.
- gRPC/role/WS đã fix (chờ redeploy).

## Ngân sách tài nguyên mục tiêu

| Kịch bản | RAM | Đánh giá |
|---|---|---|
| App stack đơn thuần (12 JVM + 2 MySQL + KC + MQ + MinIO) | ~7.3 GB | ✅ thoải mái /16GB |
| + Observability (Jaeger/Prom/Grafana/Loki) | ~8.7 GB | ✅ ổn |
| + CI (Jenkins + SonarQube) chạy CÙNG LÚC | ~14 GB | ⚠️ sát trần → tách ra |

**Nguyên tắc:** App + Observability ở chung VPS = OK. **CI (Jenkins/SonarQube) phải tách hoặc không chạy đồng thời lúc serve/deploy.**

---

## Phase 0 — Redeploy fix hiện có + đo baseline (BẮT BUỘC trước)

Mọi fix đã commit (gRPC, start_period, resilience4j) chưa có hiệu lực vì chưa rebuild image.

```bash
git push -u origin tnguyen/member1
./mvnw clean package -DskipTests
docker compose build && docker compose up -d
```
- [ ] Đo: thời gian tạo server/channel, nhắn nhóm, startup mỗi service.
- [ ] `docker logs <svc> | grep "No servers found"` → RỖNG.
- [ ] `docker stats --no-stream` → ghi lại RAM/CPU thực tế từng container.

→ Nhiều khả năng lag chức năng (create/chat/role/WS) **hết** sau bước này. Phase 1+ xử lý lag/giật còn lại.

---

## Phase 1 — Tối ưu config (KHÔNG đổi hạ tầng, làm ngay)

### 1.1 JVM: tăng tốc startup, giảm CPU spike
Sửa `JAVA_OPTS` trong docker-compose (mọi app service):
```
-XX:+UseSerialGC -XX:TieredStopAtLevel=1 -Xss512k -XX:+ExitOnOutOfMemoryError -Xmx<giữ nguyên>
```
- `TieredStopAtLevel=1`: chỉ JIT C1 → **startup nhanh hơn nhiều**, ít ngốn CPU (đánh đổi peak throughput — chấp nhận được ở scale này).
- `SerialGC`: giữ (tốt cho heap nhỏ + ít core, ít thread GC tranh CPU).
- `-Xss512k`: giảm RAM theo thread.

### 1.2 Lazy initialization (startup nhanh + nhẹ RAM lúc boot)
Mỗi `application.yml`:
```yaml
spring:
  main:
    lazy-initialization: true
```
Đánh đổi: request đầu tiên tới mỗi bean chậm hơn chút.

### 1.3 Giảm tracing sampling 100% → 10%
Mọi service:
```yaml
management:
  tracing:
    sampling:
      probability: 0.1
```
→ giảm ~90% overhead trace (CPU + I/O export OTLP).

### 1.4 Stagger startup (giải nút thắt #1)
Chia khởi động theo đợt qua `depends_on` để KHÔNG cold-start 12 JVM cùng lúc:
- Đợt 1 (hạ tầng): mysql, keycloak-db, rabbitmq, minio, keycloak
- Đợt 2 (core): auth, server, channel, role, user-profile
- Đợt 3 (phụ thuộc): messaging, presence, notification, file, friend, log
- Đợt 4: gateway
→ thêm `depends_on` cho đợt sau trỏ đợt trước (service_started, không cần healthy để tránh chờ lâu).

### 1.5 mem_limit chặn service ngốn RAM
Mỗi app service thêm:
```yaml
mem_limit: <Xmx + 256m>   # vd 192m heap -> 448m; messaging 384m -> 640m
```
→ tránh 1 JVM phình làm OOM cả host.

### 1.6 Gộp DB Keycloak vào MySQL app (bỏ 1 container)
Keycloak trỏ vào `mysql-db` (database riêng `keycloak`) thay vì `keycloak-db` riêng → tiết kiệm ~400–500MB + 1 container.

**Ước tính sau Phase 1:** startup giảm còn ~40–70s/service, RAM app ~6.5GB, hết deploy-fail, giảm giật.

---

## Phase 2 — Hạ tầng (xử lý nút thắt #2, hiệu quả lớn nhất)

### 2.1 Tách CI ra khỏi VPS app  ⭐ ưu tiên cao nhất
- Chuyển build/scan sang **GitHub Actions** (hoặc 1 VPS/CI riêng), VPS app **chỉ pull image + deploy**.
- Giải phóng **~5–6 GB RAM + CPU** khỏi VPS app → 16GB dư thoải mái.
- Nếu bắt buộc giữ Jenkins/Sonar trên VPS: **chỉ chạy khi KHÔNG serve** (lịch build ban đêm), không build đồng thời lúc deploy.

### 2.2 Tinh chỉnh observability
- Prometheus `scrape_interval: 30s` (thay vì 15s mặc định).
- Loki/Jaeger giới hạn retention (vd 24–48h) để khỏi phình đĩa/RAM.

---

## Phase 3 — App-level (tùy chọn, sau khi ổn định)

- 3.1 gRPC deadline: thêm `withDeadlineAfter(3, SECONDS)` trong các adapter → không treo thread khi downstream chậm.
- 3.2 Tạo channel/role mặc định **bất đồng bộ** (`@Async`) trong create-server → trả về ngay, không chờ gRPC.
- 3.3 Cache kết quả `getServerDetails`/`getPermissions` ngắn hạn (Caffeine) → giảm số gRPC call lặp.

---

## Thứ tự thực hiện & tác động

| Bước | Công sức | Tác động | Ưu tiên |
|---|---|---|---|
| Phase 0 redeploy | Thấp | Hết lag chức năng (gRPC) | 🔴 Ngay |
| 1.4 stagger + 1.1 JVM | TB | Hết deploy-fail, startup nhanh | 🔴 Cao |
| 2.1 tách CI | TB (hạ tầng) | Hết giật chung (RAM/CPU) | 🔴 Cao nhất cho lag |
| 1.3 sampling + 1.6 gộp DB + 1.5 mem_limit | Thấp | Giảm tải đều | 🟠 |
| 1.2 lazy-init | Thấp | Startup nhẹ | 🟠 |
| Phase 3 | Cao (sửa code) | Mượt hơn | 🟡 Sau |

## Tiêu chí hoàn thành

- [ ] `docker compose up -d` không còn service unhealthy / deploy fail.
- [ ] Startup mỗi service < 70s.
- [ ] Tạo server/channel < 2s; nhắn nhóm tức thì.
- [ ] `docker stats` lúc tải: RAM < 12GB, không OOMKilled, CPU không pin 100% kéo dài.
- [ ] `docker logs | grep -i "No servers found\|UNAVAILABLE"` rỗng.

## Câu hỏi còn mở

1. CI (Jenkins/SonarQube) bắt buộc chung VPS app không? ĐÃ TÁCH SONARQUBE → quyết định Phase 2.1.
2. Có thể dùng GitHub Actions build image thay Jenkins-on-VPS không? KHÔNG, ĐÃ DÙNG ZENKIN CHO NHANH
3. Observability có cần chạy 24/7 hay chỉ bật khi cần debug? CHẠY 24/7
