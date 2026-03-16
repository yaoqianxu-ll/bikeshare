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

 Date: 10/03/2026 17:57:11
*/

-- 创建数据库
CREATE DATABASE IF NOT EXISTS bickdemo CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

USE bickdemo;
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
) ENGINE = InnoDB AUTO_INCREMENT = 11 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_0900_ai_ci COMMENT = '背景图片表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Records of background_images
-- ----------------------------
INSERT INTO `background_images` VALUES (1, '默认背景 1', '', 'DEFAULT', 0, 1, '2026-03-09 18:25:11', '2026-03-09 18:25:11', 0);
INSERT INTO `background_images` VALUES (10, 'tiank.jpg', 'http://localhost:9000/bicycles/473de4c7-176c-494c-a749-bf2212f77c70.jpg', 'CUSTOM', 1, 0, '2026-03-10 17:50:06', '2026-03-10 17:50:06', 0);

-- ----------------------------
-- Table structure for bicycles
-- ----------------------------
DROP TABLE IF EXISTS `bicycles`;
CREATE TABLE `bicycles`  (
                             `id` bigint NOT NULL AUTO_INCREMENT COMMENT '自行车 ID',
                             `name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '自行车名称',
                             `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '类型：MOUNTAIN/ROAD/CITY/ELECTRIC/TANDEM',
                             `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'AVAILABLE' COMMENT '状态：AVAILABLE/RENTED/MAINTENANCE/DISABLED',
                             `quantity` int NOT NULL DEFAULT 1 COMMENT '数量（库存）',
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
INSERT INTO `bicycles` VALUES (1, '山地车 X1', 'MOUNTAIN', 'AVAILABLE', '北京市朝阳区', '专业山地自行车，适合越野骑行', 25.00, 'http://localhost:9000/bicycles/5fe1bdd5-37ef-4b9c-8705-c6ce7882cfb5.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (2, '公路车 Pro', 'ROAD', 'AVAILABLE', '北京市海淀区', '轻量化公路车，适合长途骑行', 35.00, 'http://localhost:9000/bicycles/c0460e9b-bbbb-47c7-a89a-5544b961e908.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (3, '城市单车 C1', 'CITY', 'AVAILABLE', '北京市东城区', '舒适城市自行车，适合日常通勤', 15.00, 'http://localhost:9000/bicycles/978e2c03-2a78-4085-bb9f-5d9d61aecff2.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (4, '电动车 E1', 'ELECTRIC', 'AVAILABLE', '北京市西城区', '电动助力自行车，省力便捷', 30.00, 'http://localhost:9000/bicycles/001f8da5-b139-406b-81ad-42b7c61c5f2b.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (5, '双人车 T1', 'TANDEM', 'AVAILABLE', '北京市丰台区', '双人协力自行车，适合情侣朋友', 40.00, 'http://localhost:9000/bicycles/5628005a-afdc-4172-9ee9-f0e55d370b04.jpg', '2026-03-03 18:50:58', '2026-03-03 18:50:58', 0);
INSERT INTO `bicycles` VALUES (6, '机车', 'ROAD', 'MAINTENANCE', '江西省九江市', '公路车最快速度', 50.00, 'http://localhost:9000/bicycles/fe89ad6b-920b-45fe-9150-ec0a48d3fe9f.jpg', '2026-03-08 18:10:08', '2026-03-08 18:10:08', 0);
INSERT INTO `bicycles` VALUES (7, '自行车', 'CITY', 'AVAILABLE', '江西省九江市', '单车更方便出行，绝不堵车，畅通无阻', 5.00, 'http://localhost:9000/bicycles/d2ff510e-fdbc-42df-815f-e53a94d27994.jpg', '2026-03-09 18:00:03', '2026-03-09 18:00:03', 0);
INSERT INTO `bicycles` VALUES (8, '劳斯莱斯幻影', 'CITY', 'DISABLED', '上海市外滩区', '享受不一样的体验', 188.00, 'http://localhost:9000/bicycles/5ab8287c-7802-42d3-8cd3-f6d94162956e.jpg', '2026-03-09 18:04:46', '2026-03-09 18:04:46', 0);

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
                            `quantity` int NOT NULL DEFAULT 1 COMMENT '租赁数量',
                            `total_price` decimal(10, 2) NULL DEFAULT NULL COMMENT '总价格',
                            `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
                            `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
                            `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                            PRIMARY KEY (`id`) USING BTREE,
                            INDEX `idx_user_id`(`user_id` ASC) USING BTREE,
                            INDEX `idx_bicycle_id`(`bicycle_id` ASC) USING BTREE,
                            INDEX `idx_status`(`status` ASC) USING BTREE
) ENGINE = InnoDB AUTO_INCREMENT = 36 CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '租赁记录表' ROW_FORMAT = Dynamic;

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
INSERT INTO `rentals` VALUES (35, 2, 1, '2026-03-09 22:01:58', '2026-03-09 22:03:32', '2026-03-09 23:01:57', 'COMPLETED', 0.65, '2026-03-09 22:01:58', '2026-03-09 22:01:58', 0);

-- ----------------------------
-- Table structure for email_auth
-- ----------------------------
DROP TABLE IF EXISTS `email_auth`;
CREATE TABLE `email_auth`  (
                               `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
                               `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
                               `verify_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验证码',
                               `code_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验证码用途：REGISTER/RESET_PASSWORD/UPDATE_EMAIL',
                               `code_expire_at` datetime NULL DEFAULT NULL COMMENT '验证码过期时间',
                               `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
                               `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
                               `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
                               PRIMARY KEY (`id`) USING BTREE,
                               UNIQUE INDEX `uk_email_auth_email`(`email` ASC) USING BTREE
) ENGINE = InnoDB CHARACTER SET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT = '邮箱验证码表' ROW_FORMAT = Dynamic;

-- ----------------------------
-- Table structure for users
-- ----------------------------
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users`  (
                          `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户 ID',
                          `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '用户名',
                          `password` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '密码',
                          `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
                          `avatar` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '头像 URL',
                          `bio` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个人简介',
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
INSERT INTO `users` VALUES (1, 'admin', '$2a$10$b9Z8YSgrEKt7mRaJtgr2rOpCehkPOM3.dc5gvJ5Md6D.Rpx3MS5CK', 'admin@example.com', NULL, '系统管理员，负责平台整体运营。', 'ADMIN', 1, '2026-03-08 17:16:36', '2026-03-08 17:16:36', 0);
INSERT INTO `users` VALUES (2, 'user', '$2a$10$d1nzggZk4u2S5aLMTanjFeB5XPuwpTEr8xM11sdDdjSARgsrRHW2.', 'user@example.com', NULL, '热爱城市骑行的默认体验用户。', 'USER', 1, '2026-03-08 17:15:54', '2026-03-08 17:15:54', 0);
INSERT INTO `users` VALUES (3, 'test', '$2a$10$Z14viQZe4IGLeuv150JeyeL6CUinkFSSnVPdgLR9GE8GjBHDpw5ny', 'test@example.com', NULL, '喜欢探索不同路线的测试账号。', 'USER', 1, '2026-03-08 16:46:08', '2026-03-08 16:46:08', 0);

-- ----------------------------
-- Table structure for friend_requests
-- ----------------------------
DROP TABLE IF EXISTS `friend_requests`;
CREATE TABLE `friend_requests` (
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

-- ----------------------------
-- Table structure for friendships
-- ----------------------------
DROP TABLE IF EXISTS `friendships`;
CREATE TABLE `friendships` (
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

-- ----------------------------
-- Table structure for chat_messages
-- ----------------------------
DROP TABLE IF EXISTS `chat_messages`;
CREATE TABLE `chat_messages` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '私信消息 ID',
  `sender_id` bigint NOT NULL COMMENT '发送者用户 ID',
  `receiver_id` bigint NOT NULL COMMENT '接收者用户 ID',
  `type` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'TEXT' COMMENT '消息类型：TEXT/EMOJI/IMAGE',
  `content` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '消息内容',
  `media_url` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '图片地址',
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

-- ----------------------------
-- Table structure for forum_posts
-- ----------------------------
DROP TABLE IF EXISTS `forum_posts`;
CREATE TABLE `forum_posts` (
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

-- ----------------------------
-- Table structure for forum_post_comments
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_comments`;
CREATE TABLE `forum_post_comments` (
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

-- ----------------------------
-- Table structure for forum_post_reactions
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_reactions`;
CREATE TABLE `forum_post_reactions` (
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

-- ----------------------------
-- Table structure for forum_post_images
-- ----------------------------
DROP TABLE IF EXISTS `forum_post_images`;
CREATE TABLE `forum_post_images` (
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

-- ----------------------------
-- Table structure for login_logs
-- ----------------------------
DROP TABLE IF EXISTS `login_logs`;
CREATE TABLE `login_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '登录日志 ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户 ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `login_method` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'USERNAME' COMMENT '登录方式：USERNAME/EMAIL',
  `login_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录 IP',
  `login_address` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '登录地址',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAIL',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果说明',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器信息',
  `login_time` datetime DEFAULT NULL COMMENT '登录时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_login_logs_username` (`username`) USING BTREE,
  KEY `idx_login_logs_status` (`status`) USING BTREE,
  KEY `idx_login_logs_time` (`login_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='登录日志表';

-- ----------------------------
-- Table structure for operation_logs
-- ----------------------------
DROP TABLE IF EXISTS `operation_logs`;
CREATE TABLE `operation_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '操作日志 ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户 ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `role_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `module` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作模块',
  `operation_name` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作名称',
  `operation_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作类型：查询/新增/修改/删除/审核',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求地址',
  `operation_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作 IP',
  `operation_address` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '操作地址',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAIL',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果说明',
  `request_params` text CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci COMMENT '请求参数',
  `duration_ms` bigint DEFAULT 0 COMMENT '耗时（毫秒）',
  `operation_time` datetime DEFAULT NULL COMMENT '操作时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_operation_logs_username` (`username`) USING BTREE,
  KEY `idx_operation_logs_role` (`role_name`) USING BTREE,
  KEY `idx_operation_logs_module` (`module`) USING BTREE,
  KEY `idx_operation_logs_type` (`operation_type`) USING BTREE,
  KEY `idx_operation_logs_status` (`status`) USING BTREE,
  KEY `idx_operation_logs_time` (`operation_time`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';

-- ----------------------------
-- Table structure for visit_logs
-- ----------------------------
DROP TABLE IF EXISTS `visit_logs`;
CREATE TABLE `visit_logs` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '访客日志 ID',
  `user_id` bigint DEFAULT NULL COMMENT '用户 ID',
  `username` varchar(50) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '用户名',
  `role_name` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '角色',
  `request_method` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求方法',
  `request_uri` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '请求地址',
  `visit_ip` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问 IP',
  `visit_address` varchar(128) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '访问地址',
  `status` varchar(20) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL DEFAULT 'SUCCESS' COMMENT '状态：SUCCESS/FAIL/BLOCKED',
  `status_code` int DEFAULT NULL COMMENT '响应状态码',
  `duration_ms` bigint DEFAULT 0 COMMENT '耗时（毫秒）',
  `user_agent` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '浏览器信息',
  `message` varchar(255) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci DEFAULT NULL COMMENT '结果说明',
  `visited_at` datetime DEFAULT NULL COMMENT '访问时间',
  `created_at` datetime DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` datetime DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  `deleted` tinyint(1) DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  KEY `idx_visit_logs_username` (`username`) USING BTREE,
  KEY `idx_visit_logs_method` (`request_method`) USING BTREE,
  KEY `idx_visit_logs_ip` (`visit_ip`) USING BTREE,
  KEY `idx_visit_logs_status` (`status`) USING BTREE,
  KEY `idx_visit_logs_time` (`visited_at`) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='访客日志表';

SET FOREIGN_KEY_CHECKS = 1;

