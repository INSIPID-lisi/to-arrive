CREATE TABLE `orders` (
                          `id`             BIGINT UNSIGNED NOT NULL COMMENT '订单ID（雪花ID，MP ASSIGN_ID生成）',
                          `order_no`       VARCHAR(20)     NOT NULL COMMENT '订单编号：TA + yyyyMMddHHmmss + 4位随机数字',
                          `passenger_id`   BIGINT UNSIGNED NOT NULL COMMENT '乘客用户ID',
                          `driver_id`      BIGINT UNSIGNED DEFAULT NULL COMMENT '司机用户ID（接单前为空）',
                          `start_lng`      DOUBLE          NOT NULL COMMENT '起点经度',
                          `start_lat`      DOUBLE          NOT NULL COMMENT '起点纬度',
                          `start_address`  VARCHAR(255)    DEFAULT NULL COMMENT '起点地址',
                          `end_lng`        DOUBLE          NOT NULL COMMENT '终点经度',
                          `end_lat`        DOUBLE          NOT NULL COMMENT '终点纬度',
                          `end_address`    VARCHAR(255)    DEFAULT NULL COMMENT '终点地址',
                          `status`         TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1待接单 2已接单 3已到达 4行程中 5已完成 6已取消',
                          `distance`       DECIMAL(10,2)   DEFAULT NULL COMMENT '行程距离（km）',
                          `duration`       INT             DEFAULT NULL COMMENT '行程时长（分钟）',
                          `estimate_price` DECIMAL(10,2)   DEFAULT NULL COMMENT '预估价格（元）',
                          `actual_price`   DECIMAL(10,2)   DEFAULT NULL COMMENT '实际价格（元）',
                          `cancel_reason`  VARCHAR(255)    DEFAULT NULL COMMENT '取消原因',
                          `cancel_by`      TINYINT         DEFAULT NULL COMMENT '取消方：1乘客 2司机 3系统',
                          `created_at`     DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `accepted_at`    DATETIME        DEFAULT NULL COMMENT '接单时间',
                          `arrived_at`     DATETIME        DEFAULT NULL COMMENT '司机到达上车点时间',
                          `started_at`     DATETIME        DEFAULT NULL COMMENT '行程开始时间',
                          `finished_at`    DATETIME        DEFAULT NULL COMMENT '行程结束时间',
                          `cancelled_at`   DATETIME        DEFAULT NULL COMMENT '订单取消时间',
                          `delete_at`      DATETIME        NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '逻辑删除墓碑：1970-01-01 00:00:00表示未删除，删除时写入now()',
                          PRIMARY KEY (`id`),
                          UNIQUE KEY `uk_order_no` (`order_no`),
                          KEY `idx_passenger_id` (`passenger_id`),
                          KEY `idx_driver_id` (`driver_id`),
                          KEY `idx_status_created_at` (`status`, `created_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='订单表';

ALTER TABLE `orders`
    ADD COLUMN `pay_status` TINYINT  NOT NULL DEFAULT 0 COMMENT '支付状态：0未支付 1已支付 2支付中' AFTER `actual_price`,
    ADD COLUMN `paid_at`    DATETIME DEFAULT NULL COMMENT '支付时间' AFTER `pay_status`;

ALTER TABLE `orders`
    ADD COLUMN `paying_at` DATETIME DEFAULT NULL COMMENT '支付发起时间（支付中状态使用）' AFTER `paid_at`;

