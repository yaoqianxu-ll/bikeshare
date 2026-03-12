USE bickdemo;

CREATE TABLE IF NOT EXISTS `email_auth` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  `email` varchar(100) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NOT NULL COMMENT '邮箱',
  `verify_code` varchar(10) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验证码',
  `code_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验证码用途：REGISTER/RESET_PASSWORD',
  `code_expire_at` datetime NULL DEFAULT NULL COMMENT '验证码过期时间',
  `created_at` datetime NULL DEFAULT NULL COMMENT '创建时间',
  `updated_at` datetime NULL DEFAULT NULL COMMENT '更新时间',
  `deleted` tinyint(1) NULL DEFAULT 0 COMMENT '逻辑删除：0-未删除，1-已删除',
  PRIMARY KEY (`id`) USING BTREE,
  UNIQUE INDEX `uk_email_auth_email`(`email` ASC) USING BTREE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='邮箱验证码表';

INSERT INTO `email_auth` (`email`, `verify_code`, `code_type`, `code_expire_at`, `created_at`, `updated_at`, `deleted`)
SELECT q.`qq_email`, q.`verify_code`, q.`code_type`, q.`code_expire_at`, q.`created_at`, q.`updated_at`, q.`deleted`
FROM `qq_email_auth` q
LEFT JOIN `email_auth` e ON e.`email` = q.`qq_email`
WHERE e.`id` IS NULL;

ALTER TABLE `users` DROP COLUMN IF EXISTS `phone`;

DROP TABLE IF EXISTS `qq_email_auth`;
