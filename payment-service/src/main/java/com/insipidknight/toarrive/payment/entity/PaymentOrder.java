package com.insipidknight.toarrive.payment.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.insipidknight.toarrive.payment.entity.enums.PaymentOrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单结算单：order_no 是支付幂等键，同时固化本次分账金额。
 */
@Data
@TableName("payment_order")
public class PaymentOrder {

    @TableId(value = "order_no", type = IdType.INPUT)
    private String orderNo;

    private Long passengerId;

    private Long driverId;

    private BigDecimal amount;

    private BigDecimal commissionRate;

    private BigDecimal commission;

    private BigDecimal driverIncome;

    private PaymentOrderStatus status;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
