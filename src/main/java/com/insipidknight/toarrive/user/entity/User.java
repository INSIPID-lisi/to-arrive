package com.insipidknight.toarrive.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.insipidknight.toarrive.user.entity.enums.UserRole;
import com.insipidknight.toarrive.user.entity.enums.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;


@Data
@AllArgsConstructor
@NoArgsConstructor
public class User {

    //主键雪花id
    @TableId(type = IdType.ASSIGN_ID)
    private Long userId;

    //对外展示的12位uid
    private String uid;

    private String phone;

    private UserRole role;

    private UserStatus status;

    private String password;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;


    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "'1970-01-01 00:00:00'", delval = "now()")
    private LocalDateTime deleteAt;
}
