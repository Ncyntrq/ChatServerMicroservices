-- Flyway V1 baseline schema for messaging-service (chat_messaging_db)
-- Auto-generated from Hibernate (ddl-auto=create). FK checks disabled for create order.
SET FOREIGN_KEY_CHECKS=0;

CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `channel_id` bigint DEFAULT NULL,
  `content` text COLLATE utf8mb4_unicode_ci,
  `is_deleted` bit(1) DEFAULT NULL,
  `is_edited` bit(1) DEFAULT NULL,
  `receiver` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `reply_to_message_id` bigint DEFAULT NULL,
  `sender` varchar(255) COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  `server_id` bigint DEFAULT NULL,
  `timestamp` datetime(6) DEFAULT NULL,
  `type` enum('CHAT','PRIVATE','SYSTEM','ERROR','LIST','JOIN','LEAVE','PING','PONG','EDIT','DELETE','TYPING','STATUS','REACT') COLLATE utf8mb4_unicode_ci DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `idx_chat_messages_channel_id` (`channel_id`),
  KEY `idx_chat_messages_sender` (`sender`),
  KEY `idx_chat_messages_receiver` (`receiver`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
CREATE TABLE IF NOT EXISTS `message_reactions` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `reaction_count` int NOT NULL,
  `emoji` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  `message_id` bigint NOT NULL,
  `user_id` varchar(255) COLLATE utf8mb4_unicode_ci NOT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `idx_message_reactions_unique` (`message_id`,`user_id`,`emoji`),
  KEY `idx_message_reactions_message_id` (`message_id`),
  CONSTRAINT `FKl0wgr4m59s18ykhnsp6a55icw` FOREIGN KEY (`message_id`) REFERENCES `chat_messages` (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

SET FOREIGN_KEY_CHECKS=1;
