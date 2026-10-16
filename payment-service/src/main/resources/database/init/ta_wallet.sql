CREATE TABLE `wallet` (
                          `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID（1:1 关联 user）',
                          `balance`     DECIMAL(10,2)   NOT NULL DEFAULT 0.00 COMMENT '可用余额（元）',
                          `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          PRIMARY KEY (`user_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='用户钱包';
