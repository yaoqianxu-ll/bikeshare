USE bickdemo;

-- 创建 IP 黑名单表
CREATE TABLE IF NOT EXISTS ip_blacklists (
  id BIGINT NOT NULL AUTO_INCREMENT COMMENT '主键 ID',
  ip VARCHAR(50) NOT NULL COMMENT 'IP 地址',
  address VARCHAR(255) DEFAULT NULL COMMENT 'IP 归属地',
  reason VARCHAR(500) DEFAULT NULL COMMENT '封禁原因',
  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE-封禁中，EXPIRED-已过期',
  created_at DATETIME NOT NULL COMMENT '封禁时间',
  expire_at DATETIME NOT NULL COMMENT '到期时间',
  updated_at DATETIME DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  deleted TINYINT(1) DEFAULT 0 COMMENT '逻辑删除',
  PRIMARY KEY (id),
  KEY idx_ip_blacklists_ip (ip),
  KEY idx_ip_blacklists_status (status),
  KEY idx_ip_blacklists_expire_at (expire_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='IP 黑名单表';
