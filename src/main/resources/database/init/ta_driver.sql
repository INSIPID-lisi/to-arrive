CREATE TABLE `driver` (
                          `driver_id`     BIGINT UNSIGNED NOT NULL COMMENT '司机ID（= 用户ID，1:1 关联 user）',
                          `real_name`     VARCHAR(50)     NOT NULL COMMENT '真实姓名',
                          `id_card_no`    VARCHAR(20)     NOT NULL COMMENT '身份证号',
                          `license_no`    VARCHAR(20)     NOT NULL COMMENT '驾驶证号',
                          `license_img`   VARCHAR(255)    DEFAULT NULL COMMENT '驾驶证照片URL',
                          `verify_status` TINYINT         NOT NULL DEFAULT 1 COMMENT '审核状态：1待审核 2已通过 3已驳回',
                          `verify_at`     DATETIME        DEFAULT NULL COMMENT '审核时间',
                          `reject_reason` VARCHAR(255)    DEFAULT NULL COMMENT '驳回原因',
                          `plate_number`  VARCHAR(20)     NOT NULL COMMENT '车牌号',
                          `work_status`   TINYINT         NOT NULL DEFAULT 0 COMMENT '工作状态：0收车 1出车',
                          `create_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                          `update_time`   DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                          `delete_at`     DATETIME        NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '逻辑删除墓碑：1970-01-01 00:00:00表示未删除，删除时写入now()',
                          PRIMARY KEY (`driver_id`),
                          UNIQUE KEY `uk_plate_number` (`plate_number`, `delete_at`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
    COMMENT='司机表';
