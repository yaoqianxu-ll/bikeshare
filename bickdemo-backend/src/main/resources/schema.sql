-- 自行车租借系统数据库脚本
-- MySQL 8+

-- 创建数据库
CREATE DATABASE IF NOT EXISTS bickdemo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE bickdemo;

-- 用户表
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
    `username` VARCHAR(50) NOT NULL COMMENT '用户名',
    `password` VARCHAR(255) NOT NULL COMMENT '密码',
    `email` VARCHAR(100) NOT NULL COMMENT '邮箱',
    `phone` VARCHAR(100) DEFAULT NULL COMMENT '手机号',
    `avatar` VARCHAR(255) DEFAULT NULL COMMENT '头像 URL',
    `role` VARCHAR(20) NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
    `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_username` (`username`),
    UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 自行车表
DROP TABLE IF EXISTS `bicycles`;
CREATE TABLE `bicycles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '自行车 ID',
    `name` VARCHAR(100) NOT NULL COMMENT '自行车名称',
    `type` VARCHAR(20) NOT NULL COMMENT '类型：MOUNTAIN/ROAD/CITY/ELECTRIC/TANDEM',
    `status` VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/RENTED/MAINTENANCE/DISABLED',
    `location` VARCHAR(255) DEFAULT NULL COMMENT '位置',
    `description` TEXT DEFAULT NULL COMMENT '描述',
    `price_per_hour` DECIMAL(10,2) DEFAULT NULL COMMENT '每小时价格',
    `image_url` VARCHAR(500) DEFAULT NULL COMMENT '图片 URL',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='自行车表';

-- 租赁记录表
DROP TABLE IF EXISTS `rentals`;
CREATE TABLE `rentals` (
    `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '租赁记录 ID',
    `user_id` BIGINT NOT NULL COMMENT '用户 ID',
    `bicycle_id` BIGINT NOT NULL COMMENT '自行车 ID',
    `start_time` DATETIME NOT NULL COMMENT '租赁开始时间',
    `end_time` DATETIME DEFAULT NULL COMMENT '租赁结束时间',
    `expected_end_time` DATETIME DEFAULT NULL COMMENT '预计归还时间',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/COMPLETED/CANCELLED',
    `total_price` DECIMAL(10,2) DEFAULT NULL COMMENT '总价格',
    `created_at` DATETIME DEFAULT NULL COMMENT '创建时间',
    `updated_at` DATETIME DEFAULT NULL COMMENT '更新时间',
    `deleted` TINYINT(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
    PRIMARY KEY (`id`),
    KEY `idx_user_id` (`user_id`),
    KEY `idx_bicycle_id` (`bicycle_id`),
    KEY `idx_status` (`status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='租赁记录表';

-- 初始化数据

-- 插入默认管理员账号 (密码：admin123)
INSERT INTO `users` (`username`, `password`, `email`, `role`, `enabled`, `created_at`, `updated_at`, `deleted`)
VALUES ('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iXS9bXS/CpQp4GPSSPBgvPJ6Cj8e', 'admin@example.com', 'ADMIN', 1, NOW(), NOW(), 0);

-- 插入测试用户 (密码：user123)
INSERT INTO `users` (`username`, `password`, `email`, `role`, `enabled`, `created_at`, `updated_at`, `deleted`)
VALUES ('user', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iXS9bXS/CpQp4GPSSPBgvPJ6Cj8e', 'user@example.com', 'USER', 1, NOW(), NOW(), 0);

-- 插入测试自行车数据
INSERT INTO `bicycles` (`name`, `type`, `status`, `location`, `description`, `price_per_hour`, `image_url`, `created_at`, `updated_at`, `deleted`)
VALUES
('山地车 X1', 'MOUNTAIN', 'AVAILABLE', '北京市朝阳区', '专业山地自行车，适合越野骑行', 25.00, 'https://via.placeholder.com/300x200?text=Mountain+Bike', NOW(), NOW(), 0),
('公路车 Pro', 'ROAD', 'AVAILABLE', '北京市海淀区', '轻量化公路车，适合长途骑行', 35.00, 'https://via.placeholder.com/300x200?text=Road+Bike', NOW(), NOW(), 0),
('城市单车 C1', 'CITY', 'AVAILABLE', '北京市东城区', '舒适城市自行车，适合日常通勤', 15.00, 'https://via.placeholder.com/300x200?text=City+Bike', NOW(), NOW(), 0),
('电动车 E1', 'ELECTRIC', 'AVAILABLE', '北京市西城区', '电动助力自行车，省力便捷', 30.00, 'https://via.placeholder.com/300x200?text=Electric+Bike', NOW(), NOW(), 0),
('双人车 T1', 'TANDEM', 'AVAILABLE', '北京市丰台区', '双人协力自行车，适合情侣朋友', 40.00, 'https://via.placeholder.com/300x200?text=Tandem+Bike', NOW(), NOW(), 0);
