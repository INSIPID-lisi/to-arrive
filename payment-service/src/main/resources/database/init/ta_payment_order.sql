CREATE TABLE `payment_order` (
                                  `order_no`        VARCHAR(20)     NOT NULL COMMENT '订单编号（支付幂等键）',
                                  `passenger_id`    BIGINT UNSIGNED NOT NULL COMMENT '付款乘客ID',
                                  `driver_id`       BIGINT UNSIGNED NOT NULL COMMENT '收款司机ID',
                                  `amount`          DECIMAL(10,2)   NOT NULL COMMENT '订单金额（元）',
                                  `commission_rate` DECIMAL(5,4)    NOT NULL COMMENT '平台抽成比例',
                                  `commission`      DECIMAL(10,2)   NOT NULL COMMENT '平台抽成金额（元）',
                                  `driver_income`   DECIMAL(10,2)   NOT NULL COMMENT '司机收入（元）',
                                  `status`          TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1处理中 2成功 3失败',
                                  `create_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                                  `update_time`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                                  PRIMARY KEY (`order_no`),
                                  KEY `idx_driver_id` (`driver_id`),
                                  KEY `idx_create_time` (`create_time`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='订单支付结算单';
