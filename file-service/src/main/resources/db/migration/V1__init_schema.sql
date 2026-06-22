-- Flyway V1 baseline schema for file-service (chat_file_db)
-- Auto-generated from Hibernate (ddl-auto=create). FK checks disabled for create order.
SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE IF NOT EXISTS `file_metadata` (
  `channel_id` bigint DEFAULT NULL,
  `created_at` datetime(6) NOT NULL,
  `file_size` bigint NOT NULL,
  `id` bigint NOT NULL AUTO_INCREMENT,
  `bucket` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `content_type` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `uploader` varchar(100) COLLATE utf8mb4_unicode_ci NOT NULL,
  `original_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `stored_name` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `thumbnail_key` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK_2cqf8xb6es3rcpbwmyabis4xw` (`stored_name`),
  KEY `idx_uploader` (`uploader`),
  KEY `idx_channel` (`channel_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS=1;
