-- Bảng Transactional Outbox cho messaging-service.
-- Trước đây bảng này được sinh tự động bởi Hibernate ddl-auto=update; sau khi chuyển sang
-- ddl-auto=validate, môi trường DB mới (local) thiếu bảng -> app fail khởi động ("missing table [outbox_messages]").
-- Dùng IF NOT EXISTS để idempotent: môi trường cũ (VPS) đã có bảng sẽ bỏ qua, môi trường mới sẽ tạo.
-- Cột để snake_case khớp SpringPhysicalNamingStrategy (aggregateType -> aggregate_type ...).

CREATE TABLE IF NOT EXISTS `outbox_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `aggregate_type` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `aggregate_id` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `exchange` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `routing_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `payload` text COLLATE utf8mb4_unicode_ci,
  `status` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `created_at` datetime(6) DEFAULT NULL,
  PRIMARY KEY (`id`),
  -- Index phục vụ query relay: findTop50ByStatusOrderByCreatedAtAsc('PENDING')
  KEY `idx_outbox_status_created` (`status`, `created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
