package com.insipidknight.toarrive.user.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.insipidknight.toarrive.user.entity.enums.VerifyStatus;
import com.insipidknight.toarrive.user.entity.enums.WorkStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("driver")
public class Driver {

    // 主键 = 用户ID，1:1 挂在 user 上
    @TableId(value = "driver_id", type = IdType.INPUT)
    private Long driverId;

    private String realName;
    private String idCardNo;
    private String licenseNo;
    private String licenseImg;
    private VerifyStatus verifyStatus;
    private LocalDateTime verifyAt;
    private String rejectReason;

    private String plateNumber;
    private WorkStatus workStatus;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;

    @TableLogic(value = "'1970-01-01 00:00:00'", delval = "now()")
    private LocalDateTime deleteAt;
}
