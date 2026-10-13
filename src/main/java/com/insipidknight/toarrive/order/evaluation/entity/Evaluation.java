package com.insipidknight.toarrive.order.evaluation.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("evaluation")
public class Evaluation {

    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    private String orderNo;

    private Long passengerId;

    private Long driverId;

    private Integer score;

    private String comment;

    private String tags;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
