CREATE TABLE `evaluation` (
                              `id`           BIGINT UNSIGNED NOT NULL COMMENT '评价ID（雪花ID，MP ASSIGN_ID生成）',
                              `order_no`     VARCHAR(20)     NOT NULL COMMENT '订单编号（一单一评）',
                              `passenger_id` BIGINT UNSIGNED NOT NULL COMMENT '评价人（乘客用户ID）',
                              `driver_id`    BIGINT UNSIGNED NOT NULL COMMENT '被评司机用户ID',
                              `score`        TINYINT         NOT NULL COMMENT '评分：1-5',
                              `comment`      VARCHAR(255)    DEFAULT NULL COMMENT '文字评论',
                              `tags`         VARCHAR(255)    DEFAULT NULL COMMENT '标签（逗号分隔）',
                              `create_time`  DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                              PRIMARY KEY (`id`),
                              UNIQUE KEY `uk_order_no` (`order_no`),
                              KEY `idx_driver_id` (`driver_id`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
  COMMENT='订单评价';
