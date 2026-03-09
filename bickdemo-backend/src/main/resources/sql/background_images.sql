-- 背景图片表
CREATE TABLE IF NOT EXISTS `background_images` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `name` VARCHAR(100) NOT NULL COMMENT '图片名称',
  `image_url` VARCHAR(500) NOT NULL COMMENT '图片 URL',
  `type` VARCHAR(20) DEFAULT 'CUSTOM' COMMENT '类型：DEFAULT-默认，CUSTOM-自定义',
  `enabled` TINYINT DEFAULT 0 COMMENT '是否启用：0-否，1-是',
  `sort` INT DEFAULT 0 COMMENT '排序',
  `created_at` DATETIME DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` TINYINT DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
  PRIMARY KEY (`id`),
  KEY `idx_enabled` (`enabled`),
  KEY `idx_sort` (`sort`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='背景图片表';
