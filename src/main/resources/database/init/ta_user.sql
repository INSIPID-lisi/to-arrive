CREATE TABLE `user` (
                        `user_id`     BIGINT UNSIGNED NOT NULL COMMENT '用户ID（雪花ID，MP ASSIGN_ID生成）',
                        `phone`       VARCHAR(20)     NOT NULL COMMENT '手机号（登录凭证）',
                        `role`        TINYINT         NOT NULL DEFAULT 1 COMMENT '角色：1乘客 2司机',
                        `status`      TINYINT         NOT NULL DEFAULT 1 COMMENT '状态：1正常 0禁用 2注销',
                        `password`    VARCHAR(100)    NOT NULL COMMENT '密码哈希',
                        `create_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
                        `update_time` DATETIME        NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
                        PRIMARY KEY (`user_id`),
                        UNIQUE KEY `uk_phone` (`phone`)
) ENGINE=InnoDB
  DEFAULT CHARSET=utf8mb4
  COLLATE=utf8mb4_general_ci
    COMMENT='用户表';

ALTER TABLE `user`
    ADD COLUMN `uid` VARCHAR(12) DEFAULT NULL COMMENT '对外展示ID' AFTER `user_id`,
    ADD UNIQUE KEY `uk_uid` (`uid`);

ALTER TABLE `user`
    ADD COLUMN `delete_at` DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '逻辑删除墓碑：1970-01-01 00:00:00表示未删除，删除时写入now()';

ALTER TABLE `user`
    DROP KEY `uk_phone`,
    ADD UNIQUE KEY `uk_phone` (`phone`, `delete_at`);