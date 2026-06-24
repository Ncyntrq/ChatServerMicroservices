-- Flyway V1 baseline schema for channel-service (chat_channel_db)
-- Auto-generated from Hibernate (ddl-auto=create). FK checks disabled for create order.
SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE IF NOT EXISTS `channels` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `category` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `name` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `pinned_at` bigint DEFAULT NULL,
  `server_id` bigint DEFAULT NULL,
  `slowmode` int DEFAULT '0',
  `topic` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `type` enum('TEXT','VOICE') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS `pinned_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` bigint NOT NULL,
  `message_id` bigint NOT NULL,
  `pinned_at` datetime(6) DEFAULT NULL,
  `pinned_by` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `UK3xcr4jt1x3002l0qvoqs4stoe` (`channel_id`,`message_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS=1;
