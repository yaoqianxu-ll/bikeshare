-- ============================================
-- 骑行活动表
-- ============================================
CREATE TABLE IF NOT EXISTS `activities` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '活动ID',
    `title` VARCHAR(200) NOT NULL COMMENT '活动标题',
    `description` TEXT COMMENT '活动描述',
    `cover_image` VARCHAR(500) COMMENT '封面图片URL',
    `route` VARCHAR(500) COMMENT '骑行路线',
    `start_time` DATETIME NOT NULL COMMENT '开始时间',
    `end_time` DATETIME NOT NULL COMMENT '结束时间',
    `max_participants` INT DEFAULT 0 COMMENT '最大参与人数(0=不限)',
    `location` VARCHAR(200) COMMENT '集合地点',
    `difficulty` VARCHAR(20) DEFAULT 'EASY' COMMENT '难度: EASY/MEDIUM/HARD',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/CANCELLED/COMPLETED',
    `organizer_id` BIGINT NOT NULL COMMENT '组织者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_organizer` (`organizer_id`),
    KEY `idx_status` (`status`),
    KEY `idx_start_time` (`start_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='骑行活动';

-- ============================================
-- 活动报名表
-- ============================================
CREATE TABLE IF NOT EXISTS `activity_signups` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '报名ID',
    `activity_id` BIGINT NOT NULL COMMENT '活动ID',
    `user_id` BIGINT NOT NULL COMMENT '用户ID',
    `status` VARCHAR(20) DEFAULT 'PENDING' COMMENT '状态: PENDING/APPROVED/REJECTED/SIGNED/CANCELLED',
    `remark` VARCHAR(200) COMMENT '报名备注',
    `signed_at` DATETIME COMMENT '签到时间',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_activity_user` (`activity_id`, `user_id`),
    KEY `idx_user` (`user_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='活动报名';

-- ============================================
-- 系统公告表
-- ============================================
CREATE TABLE IF NOT EXISTS `notices` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '公告ID',
    `title` VARCHAR(200) NOT NULL COMMENT '公告标题',
    `content` TEXT NOT NULL COMMENT '公告内容',
    `type` VARCHAR(20) DEFAULT 'INFO' COMMENT '类型: INFO/WARNING/IMPORTANT',
    `cover_image` VARCHAR(500) COMMENT '封面图片',
    `status` VARCHAR(20) DEFAULT 'DRAFT' COMMENT '状态: DRAFT/PUBLISHED/HIDDEN',
    `priority` INT DEFAULT 0 COMMENT '优先级(越大越靠前)',
    `publish_time` DATETIME COMMENT '发布时间',
    `author_id` BIGINT NOT NULL COMMENT '作者ID',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_status` (`status`),
    KEY `idx_priority` (`priority`),
    KEY `idx_publish_time` (`publish_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统公告';

-- ============================================
-- 客服工单表
-- ============================================
CREATE TABLE IF NOT EXISTS `tickets` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '工单ID',
    `ticket_no` VARCHAR(32) NOT NULL COMMENT '工单编号',
    `title` VARCHAR(200) NOT NULL COMMENT '工单标题',
    `content` TEXT NOT NULL COMMENT '工单内容',
    `type` VARCHAR(50) DEFAULT 'GENERAL' COMMENT '类型: GENERAL/BUG/SUGGESTION/REFUND/COMPLAINT',
    `priority` VARCHAR(20) DEFAULT 'NORMAL' COMMENT '优先级: LOW/NORMAL/HIGH/URGENT',
    `status` VARCHAR(20) DEFAULT 'OPEN' COMMENT '状态: OPEN/ASSIGNED/PROCESSING/RESOLVED/CLOSED',
    `images` VARCHAR(1000) COMMENT '图片附件(JSON数组)',
    `user_id` BIGINT NOT NULL COMMENT '提交用户ID',
    `assignee_id` BIGINT COMMENT '处理人ID',
    `reply_content` TEXT COMMENT '回复内容',
    `reply_time` DATETIME COMMENT '回复时间',
    `resolved_time` DATETIME COMMENT '解决时间',
    `rating` TINYINT COMMENT '用户评分(1-5)',
    `feedback` VARCHAR(500) COMMENT '用户反馈',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_ticket_no` (`ticket_no`),
    KEY `idx_user` (`user_id`),
    KEY `idx_assignee` (`assignee_id`),
    KEY `idx_status` (`status`),
    KEY `idx_priority` (`priority`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='客服工单';

-- ============================================
-- 工单消息表
-- ============================================
CREATE TABLE IF NOT EXISTS `ticket_messages` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '消息ID',
    `ticket_id` BIGINT NOT NULL COMMENT '工单ID',
    `sender_id` BIGINT NOT NULL COMMENT '发送者ID',
    `sender_type` VARCHAR(20) DEFAULT 'USER' COMMENT '发送者类型: USER/ADMIN/SYSTEM',
    `content` TEXT NOT NULL COMMENT '消息内容',
    `images` VARCHAR(1000) COMMENT '图片附件',
    `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP,
    `deleted` TINYINT DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_ticket` (`ticket_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工单消息';
