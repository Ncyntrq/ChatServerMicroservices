-- =============================================================
-- Flyway V1 — presence-service (chat_presence_db)
-- =============================================================
-- Hiện tại presence-service lưu trạng thái user trong bộ nhớ
-- (ConcurrentHashMap). Bảng dưới đây chuẩn bị sẵn cho giai đoạn
-- chuyển sang persistent storage (ví dụ: crash recovery, cluster sync).
-- =============================================================

CREATE TABLE IF NOT EXISTS user_status (
    id          BIGINT          NOT NULL AUTO_INCREMENT,
    user_id     VARCHAR(100)    NOT NULL,
    status      VARCHAR(30)     NOT NULL DEFAULT 'OFFLINE'
                                COMMENT 'ONLINE | OFFLINE | AWAY | IDLE | DO_NOT_DISTURB | INVISIBLE',
    last_seen   DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    created_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    updated_at  DATETIME(6)     NOT NULL DEFAULT CURRENT_TIMESTAMP(6) ON UPDATE CURRENT_TIMESTAMP(6),

    PRIMARY KEY (id),
    UNIQUE KEY uk_user_status_user_id (user_id),
    KEY         idx_user_status_status (status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
