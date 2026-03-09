/*
 Navicat Premium Data Transfer

 Source Server         : root
 Source Server Type    : MySQL
 Source Server Version : 80043 (8.0.43)
 Source Host           : localhost:3306
 Source Schema         : bickdemo

 Target Server Type    : MySQL
 Target Server Version : 80043 (8.0.43)
 File Encoding         : 65001

 Date: 09/03/2026 21:01:12
*/
-- 创建数据库
CREATE DATABASE IF NOT EXISTS bickdemo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE bickdemo;


SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- ----------------------------
-- Table structure for background_images
-- ----------------------------
DROP TABLE IF EXISTS `background_images`;
CREATE TABLE `background_images`  (
                                      `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
                                      `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片名称',
                                      `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '图片 URL',
                                      `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NULL DEFAULT 'CUSTOM' COMMENT '类型：DEFAULT-默认，CUSTOM-自定义',
                                      `enabled` tinyint NULL DEFAULT 0 COMMENT '是否启用：0-否，1-是',
                                      `sort` int NULL DEFAULT 0 COMMENT '排序',
                                      `created_at` datetime NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      `updated_at` datetime NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                      `deleted` tinyint NULL DEFAULT 0 COMMENT '逻辑删除：0-否，1-是',
                                      PRIMARY KEY (`id`) USING BTREE,
                                      INDEX `idx_enabled`(`enabled` ASC) USING BTREE,
                                      INDEX `idx_sort`(`sort` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 9 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '背景图片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of background_images
-- ----------------------------
INSERT INTO `background_images` VALUES (1, '默认背景 1', '', 'DEFAULT', 0, 1, '2026-03-09 18:25:11', '2026-03-09 18:25:11', 0);
INSERT INTO `background_images` VALUES (6, 'th.jpg', 'http://60.205.169.251:9000/bicycles/fe3b1486-4b8c-45e0-b2f7-85bec89ea981.jpg', 'CUSTOM', 1, 0, '2026-03-09 18:34:01', '2026-03-09 18:34:01', 0);
INSERT INTO `background_images` VALUES (7, 'th.jpg', 'http://60.205.169.251:9000/bicycles/fd3eb844-5da4-4b31-8829-87156a41e5af.jpg', 'CUSTOM', 0, 0, '2026-03-09 18:35:24', '2026-03-09 18:35:29', 1);
INSERT INTO `background_images` VALUES (8, 'cd5dac76efbc694f-1.jpg', 'http://60.205.169.251:9000/bicycles/6b05ddbb-9dad-422e-9946-578a1a2d2a71.jpg', 'CUSTOM', 0, 0, '2026-03-09 18:52:35', '2026-03-09 18:52:35', 0);

-- ----------------------------
-- Table structure for bicycles
-- ----------------------------
DROP TABLE IF EXISTS `bicycles`;
CREATE TABLE `bicycles`  (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自行车 ID',
                             `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '自行车名称',
                             `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：MOUNTAIN/ROAD/CITY/ELECTRIC/TANDEM',
                             `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/RENTED/MAINTENANCE/DISABLED',
                             `location` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '位置',
                             `description` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL COMMENT '描述',
                             `price_per_hour` decimal(10, 2) NULL DEFAULT NULL COMMENT '每小时价格',
                             `image_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '图片 URL',
                             `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
                             `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
                             `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                             PRIMARY KEY (`id`) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 10 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '自行车表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of bicycles
-- ----------------------------
INSERT INTO `bicycles` VALUES (1, '山地车 X1', 'MOUNTAIN', 'AVAILABLE', '北京市朝阳区', '专业山地自行车，适合越野骑行', 25.00, 'http://60.205.169.251:9000/bicycles/f69e5ec2-bb2d-4296-912c-b071b2901d07.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (2, '公路车 Pro', 'ROAD', 'AVAILABLE', '北京市海淀区', '轻量化公路车，适合长途骑行', 35.00, 'http://60.205.169.251:9000/bicycles/9351eaeb-d2c1-4fa4-9b53-6d546a172a64.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (3, '城市单车 C1', 'CITY', 'AVAILABLE', '北京市东城区', '舒适城市自行车，适合日常通勤', 15.00, 'http://60.205.169.251:9000/bicycles/39c0019b-e10d-4d79-9ca5-0e2051c6c52f.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (4, '电动车 E1', 'ELECTRIC', 'AVAILABLE', '北京市西城区', '电动助力自行车，省力便捷', 30.00, 'http://60.205.169.251:9000/bicycles/1d34edb3-e63f-4015-bef2-38fc2ed03632.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (5, '双人车 T1', 'TANDEM', 'AVAILABLE', '北京市丰台区', '双人协力自行车，适合情侣朋友', 40.00, 'http://60.205.169.251:9000/bicycles/d11e6a83-ac65-4130-b154-4b3a5d155bf6.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (6, '机车', 'ROAD', 'MAINTENANCE', '江西省九江市', '公路车最快速度', 50.00, 'http://60.205.169.251:9000/bicycles/24d42629-9ad7-4384-a25f-1cf2189c0d2d.jpg', '2026-03-08 18:10:08', '2026-03-08 18:10:08', 0);
INSERT INTO `bicycles` VALUES (7, '自行车', 'CITY', 'AVAILABLE', '江西省九江市', '单车更方便出行，绝不堵车，畅通无阻', 5.00, 'http://60.205.169.251:9000/bicycles/22f7f0c3-de21-4c6a-a585-45f082f3d3bf.jpg', '2026-03-09 18:00:03', '2026-03-09 18:00:03', 0);
INSERT INTO `bicycles` VALUES (8, '劳斯莱斯幻影', 'CITY', 'DISABLED', '上海市外滩区', '享受不一样的体验', 188.00, 'http://60.205.169.251:9000/bicycles/11461a5e-848b-4492-ae1d-dda34edb7053.jpg', '2026-03-09 18:04:46', '2026-03-09 18:04:46', 0);

-- ----------------------------
-- Table structure for rentals
-- ----------------------------
DROP TABLE IF EXISTS `rentals`;
CREATE TABLE `rentals`  (
                            `id` bigint NOT NULL AUTO_INCREMENT COMMENT '租赁记录 ID',
                            `user_id` bigint NOT NULL COMMENT '用户 ID',
                            `bicycle_id` bigint NOT NULL COMMENT '自行车 ID',
                            `start_time` datetime NOT NULL COMMENT '租赁开始时间',
                            `end_time` datetime NULL DEFAULT NULL COMMENT '租赁结束时间',
                            `expected_end_time` datetime NULL DEFAULT NULL COMMENT '预计归还时间',
                            `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/COMPLETED/CANCELLED',
                            `total_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '总价格',
                            `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
                            `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
                            `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                            PRIMARY KEY (`id`) USING BTREE,
                            INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                            INDEX `idx_bicycle_id`(`bicycle_id` ASC) USING BTREE,
                            INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 35 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '租赁记录表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of rentals
-- ----------------------------
INSERT INTO `rentals` VALUES (1, 1, 1, '2026-03-08 18:40:13', '2026-03-08 18:42:44', '2026-03-08 19:40:08', 'COMPLETED', 1.05, '2026-03-08 18:40:13', '2026-03-08 18:40:13', 0);
INSERT INTO `rentals` VALUES (27, 1, 1, '2026-03-09 17:39:48', '2026-03-09 17:40:02', '2026-03-09 18:39:47', 'COMPLETED', 0.10, '2026-03-09 17:39:48', '2026-03-09 17:39:48', 0);
INSERT INTO `rentals` VALUES (28, 1, 2, '2026-03-09 17:39:55', '2026-03-09 17:40:04', '2026-03-10 17:39:54', 'COMPLETED', 0.08, '2026-03-09 17:39:55', '2026-03-09 17:39:55', 0);
INSERT INTO `rentals` VALUES (29, 1, 2, '2026-03-09 17:44:10', '2026-03-09 17:53:03', '2026-03-09 17:49:03', 'COMPLETED', 5.19, '2026-03-09 17:44:10', '2026-03-09 17:44:10', 0);
INSERT INTO `rentals` VALUES (30, 1, 7, '2026-03-09 18:20:34', '2026-03-09 18:26:20', '2026-03-09 19:20:33', 'COMPLETED', 0.48, '2026-03-09 18:20:34', '2026-03-09 18:20:34', 0);
INSERT INTO `rentals` VALUES (31, 2, 1, '2026-03-09 18:42:14', '2026-03-09 18:42:17', '2026-03-09 19:42:13', 'COMPLETED', 0.02, '2026-03-09 18:42:14', '2026-03-09 18:42:14', 0);
INSERT INTO `rentals` VALUES (32, 1, 2, '2026-03-09 20:26:47', '2026-03-09 20:28:42', '2026-03-09 21:26:46', 'COMPLETED', 1.11, '2026-03-09 20:26:47', '2026-03-09 20:26:47', 0);
INSERT INTO `rentals` VALUES (33, 1, 1, '2026-03-09 20:29:13', '2026-03-09 20:35:22', '2026-03-09 21:29:12', 'COMPLETED', 2.56, '2026-03-09 20:29:13', '2026-03-09 20:29:13', 0);
INSERT INTO `rentals` VALUES (34, 1, 1, '2026-03-09 20:36:56', NULL, '2026-03-09 21:36:55', 'CANCELLED', NULL, '2026-03-09 20:36:56', '2026-03-09 20:36:56', 0);

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
                          `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                          `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
                          `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
                          `phone` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '手机号',
                          `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像 URL',
                          `role` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USER' COMMENT '角色：USER/ADMIN',
                          `enabled` tinyint(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
                          `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
                          `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
                          `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                          PRIMARY KEY (`id`) USING BTREE,
                          UNIQUE INDEX `uk_username`(`username` ASC) USING BTREE,
                          UNIQUE INDEX `uk_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 6 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '用户表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of users
-- ----------------------------
INSERT INTO `users` VALUES (1, 'admin', '$2a$10$b9Z8YSgrEKt7mRaJtgr2rOpCehkPOM3.dc5gvJ5Md6D.Rpx3MS5CK', 'admin@qq.com', '14796965560', NULL, 'ADMIN', 1, '2026-03-08 17:16:36', '2026-03-08 17:16:36', 0);
INSERT INTO `users` VALUES (2, 'user', '$2a$10$d1nzggZk4u2S5aLMTanjFeB5XPuwpTEr8xM11sdDdjSARgsrRHW2.', 'user@qq.com', '18879972524', NULL, 'USER', 1, '2026-03-08 17:15:54', '2026-03-08 17:15:54', 0);
INSERT INTO `users` VALUES (3, 'test', '$2a$10$Z14viQZe4IGLeuv150JeyeL6CUinkFSSnVPdgLR9GE8GjBHDpw5ny', 'lileyaoqianxu@gmail.com', '12345678901', NULL, 'USER', 1, '2026-03-08 16:46:08', '2026-03-08 16:46:08', 0);

SET FOREIGN_KEY_CHECKS = 1;
