USE bickdemo;

SET @add_bio_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.COLUMNS
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'users'
        AND COLUMN_NAME = 'bio'
    ),
    'SELECT 1',
    "ALTER TABLE `users` ADD COLUMN `bio` varchar(500) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '个人简介' AFTER `avatar`"
  )
);
PREPARE stmt FROM @add_bio_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @update_email_auth_sql = (
  SELECT IF(
    EXISTS(
      SELECT 1
      FROM information_schema.TABLES
      WHERE TABLE_SCHEMA = DATABASE()
        AND TABLE_NAME = 'email_auth'
    ),
    "ALTER TABLE `email_auth` MODIFY COLUMN `code_type` varchar(30) CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci NULL DEFAULT NULL COMMENT '验证码用途：REGISTER/RESET_PASSWORD/UPDATE_EMAIL'",
    'SELECT 1'
  )
);
PREPARE stmt FROM @update_email_auth_sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
