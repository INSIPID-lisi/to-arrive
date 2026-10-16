CREATE TABLE `user_profile` (
                                `user_id`    BIGINT UNSIGNED NOT NULL COMMENT '用户ID',
                                `nickname`   VARCHAR(32)     DEFAULT NULL COMMENT '昵称',
                                `avatar_url` VARCHAR(255)    DEFAULT NULL COMMENT '头像',
                                `gender`     TINYINT         DEFAULT 0 COMMENT '性别：0未知 1男 2女',
                                `create_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                `update_time` DATETIME       NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                PRIMARY KEY (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户资料表';

ALTER TABLE `user_profile`
    ADD COLUMN `delete_at` DATETIME NOT NULL DEFAULT '1970-01-01 00:00:00' COMMENT '逻辑删除墓碑：1970-01-01 00:00:00表示未删除，删除时写入now()';