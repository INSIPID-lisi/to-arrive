CREATE TABLE `wallet_transaction` (
                                      `id`            BIGINT UNSIGNED NOT NULL COMMENT '流水ID（雪花ID，MP ASSIGN_ID生成）',
                                      `user_id`       BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                      `order_no`      VARCHAR(20)     DEFAULT NULL COMMENT '关联订单号（充值类为空）',
                                      `type`          TINYINT         NOT NULL COMMENT '类型：1充值 2支付',
                                      `amount`        DECIMAL(10,2)   NOT NULL COMMENT '金额（正数，方向由 type 决定）',
                                      `balance_after` DECIMAL(10,2)   NOT NULL COMMENT '变动后余额',
                                      `remark`        VARCHAR(255)    DEFAULT NULL COMMENT '备注',
                                      `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                      PRIMARY KEY (`id`),
                                      UNIQUE KEY `uk_order_no_type` (`order_no`, `type`),
                                      KEY `idx_user_id` (`user_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='钱包流水';
