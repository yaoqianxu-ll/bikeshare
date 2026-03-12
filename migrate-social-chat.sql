USE bickdemo;

CREATE TABLE IF NOT EXISTS `friend_requests` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '好友申请 ID',
  `sender_id` bigint NOT NULL COMMENT '发送者用户 ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者用户 ID',
  `remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '申请备注',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'PENDING' COMMENT '状态：PENDING/ACCEPTED/REJECTED',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_friend_requests_sender` (`sender_id`) USING BTREE,
  KEY `idx_friend_requests_receiver` (`receiver_id`) USING BTREE,
  KEY `idx_friend_requests_status` (`status`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友申请表';

CREATE TABLE IF NOT EXISTS `friendships` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '好友关系 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `friend_id` bigint NOT NULL COMMENT '好友用户 ID',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_friendships_pair` (`user_id`, `friend_id`) USING BTREE,
  KEY `idx_friendships_friend` (`friend_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='好友关系表';

CREATE TABLE IF NOT EXISTS `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '私信消息 ID',
  `sender_id` bigint NOT NULL COMMENT '发送者用户 ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者用户 ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/EMOJI/IMAGE/STICKER',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `media_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '媒体地址',
  `is_read` tinyint(1) NOT NULL DEFAULT 0 COMMENT '是否已读：0-未读，1-已读',
  `read_at` datetime DEFAULT NULL COMMENT '已读时间',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_chat_messages_sender` (`sender_id`) USING BTREE,
  KEY `idx_chat_messages_receiver` (`receiver_id`) USING BTREE,
  KEY `idx_chat_messages_read` (`is_read`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='私信消息表';

SET @chat_messages_has_read_at := (
  SELECT COUNT(*)
  FROM information_schema.COLUMNS
  WHERE TABLE_SCHEMA = DATABASE()
    AND TABLE_NAME = 'chat_messages'
    AND COLUMN_NAME = 'read_at'
);
SET @chat_messages_add_read_at_sql := IF(
  @chat_messages_has_read_at = 0,
  'ALTER TABLE `chat_messages` ADD COLUMN `read_at` datetime DEFAULT NULL COMMENT ''已读时间'' AFTER `is_read`',
  'SELECT 1'
);
PREPARE stmt_chat_messages_add_read_at FROM @chat_messages_add_read_at_sql;
EXECUTE stmt_chat_messages_add_read_at;
DEALLOCATE PREPARE stmt_chat_messages_add_read_at;
