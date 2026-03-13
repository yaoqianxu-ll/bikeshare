USE bickdemo;

CREATE TABLE IF NOT EXISTS `forum_posts` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子 ID',
  `user_id` bigint NOT NULL COMMENT '发布人用户 ID',
  `title` varchar(80) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子标题',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '帖子内容',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '帖子图片地址',
  `view_count` bigint NOT NULL DEFAULT 0 COMMENT '阅读数量',
  `like_count` bigint NOT NULL DEFAULT 0 COMMENT '点赞数量',
  `favorite_count` bigint NOT NULL DEFAULT 0 COMMENT '收藏数量',
  `comment_count` bigint NOT NULL DEFAULT 0 COMMENT '评论数量',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING/APPROVED/REJECTED',
  `reviewer_id` bigint DEFAULT NULL COMMENT '审核人用户 ID',
  `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间',
  `review_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核备注',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_forum_posts_user` (`user_id`) USING BTREE,
  KEY `idx_forum_posts_status` (`status`) USING BTREE,
  KEY `idx_forum_posts_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子表';

SET @add_forum_image_url_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'forum_posts'
        AND COLUMN_NAME = 'image_url'
    ),
    'SELECT 1',
    "ALTER TABLE `forum_posts` ADD COLUMN `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '帖子图片地址' AFTER `content`"
  )
);
PREPARE stmt FROM @add_forum_image_url_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_forum_status_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'forum_posts'
        AND COLUMN_NAME = 'status'
    ),
    'SELECT 1',
    "ALTER TABLE `forum_posts` ADD COLUMN `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'APPROVED' COMMENT '审核状态：PENDING/APPROVED/REJECTED' AFTER `comment_count`"
  )
);
PREPARE stmt FROM @add_forum_status_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_forum_reviewer_id_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'forum_posts'
        AND COLUMN_NAME = 'reviewer_id'
    ),
    'SELECT 1',
    "ALTER TABLE `forum_posts` ADD COLUMN `reviewer_id` bigint DEFAULT NULL COMMENT '审核人用户 ID' AFTER `status`"
  )
);
PREPARE stmt FROM @add_forum_reviewer_id_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_forum_reviewed_at_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'forum_posts'
        AND COLUMN_NAME = 'reviewed_at'
    ),
    'SELECT 1',
    "ALTER TABLE `forum_posts` ADD COLUMN `reviewed_at` datetime DEFAULT NULL COMMENT '审核时间' AFTER `reviewer_id`"
  )
);
PREPARE stmt FROM @add_forum_reviewed_at_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_forum_review_remark_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'forum_posts'
        AND COLUMN_NAME = 'review_remark'
    ),
    'SELECT 1',
    "ALTER TABLE `forum_posts` ADD COLUMN `review_remark` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '审核备注' AFTER `reviewed_at`"
  )
);
PREPARE stmt FROM @add_forum_review_remark_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

UPDATE `forum_posts`
SET `status` = 'APPROVED'
WHERE `status` IS NULL
   OR `status` = '';

CREATE TABLE IF NOT EXISTS `forum_post_comments` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论 ID',
  `post_id` bigint NOT NULL COMMENT '帖子 ID',
  `user_id` bigint NOT NULL COMMENT '评论人用户 ID',
  `parent_comment_id` bigint DEFAULT NULL COMMENT '父评论 ID',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '被回复的用户 ID',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '评论内容',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_forum_comments_post` (`post_id`) USING BTREE,
  KEY `idx_forum_comments_user` (`user_id`) USING BTREE,
  KEY `idx_forum_comments_parent` (`parent_comment_id`) USING BTREE,
  KEY `idx_forum_comments_created_at` (`created_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛评论表';

SET @add_forum_comment_parent_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'forum_post_comments'
        AND COLUMN_NAME = 'parent_comment_id'
    ),
    'SELECT 1',
    "ALTER TABLE `forum_post_comments` ADD COLUMN `parent_comment_id` bigint DEFAULT NULL COMMENT '父评论 ID' AFTER `user_id`"
  )
);
PREPARE stmt FROM @add_forum_comment_parent_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_forum_comment_reply_user_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'forum_post_comments'
        AND COLUMN_NAME = 'reply_to_user_id'
    ),
    'SELECT 1',
    "ALTER TABLE `forum_post_comments` ADD COLUMN `reply_to_user_id` bigint DEFAULT NULL COMMENT '被回复的用户 ID' AFTER `parent_comment_id`"
  )
);
PREPARE stmt FROM @add_forum_comment_reply_user_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

CREATE TABLE IF NOT EXISTS `forum_post_reactions` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子互动 ID',
  `post_id` bigint NOT NULL COMMENT '帖子 ID',
  `user_id` bigint NOT NULL COMMENT '用户 ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '互动类型：LIKE/FAVORITE',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_forum_reaction_pair` (`post_id`, `user_id`, `type`) USING BTREE,
  KEY `idx_forum_reactions_user` (`user_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛点赞收藏表';

CREATE TABLE IF NOT EXISTS `forum_post_images` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子图片 ID',
  `post_id` bigint NOT NULL COMMENT '帖子 ID',
  `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '图片地址',
  `sort_order` int NOT NULL DEFAULT 0 COMMENT '排序',
  `created_at` datetime DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime DEFAULT NULL COMMENT '更新时间',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE KEY `uk_forum_post_images_pair` (`post_id`, `image_url`) USING BTREE,
  KEY `idx_forum_post_images_post` (`post_id`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='论坛帖子图片表';

INSERT IGNORE INTO `forum_post_images` (`post_id`, `image_url`, `sort_order`, `created_at`, `updated_at`)
SELECT `id`, `image_url`, 0, `created_at`, `updated_at`
FROM `forum_posts`
WHERE `image_url` IS NOT NULL
  AND `image_url` <> '';
