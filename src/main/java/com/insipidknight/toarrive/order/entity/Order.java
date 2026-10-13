package com.insipidknight.toarrive.order.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.insipidknight.toarrive.order.entity.enums.CancelBy;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.entity.enums.PayStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@TableName("orders")
public class Order {
    //主键雪花id
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    //对外订单编号
    private String orderNo;

    private Long passengerId;
    private Long driverId;

    // 起终点（坐标 + 地址）
    private Double startLng;
    private Double startLat;
    private String startAddress;

    private Double endLng;
    private Double endLat;
    private String endAddress;

    private OrderStatus status;

    // 行程
    private BigDecimal distance;   // km
    private Integer duration;      // 分钟

    // 计费
    private BigDecimal estimatePrice;
    private BigDecimal actualPrice;

    // 支付
    private PayStatus payStatus;
    private LocalDateTime paidAt;

    private String cancelReason;
    private CancelBy cancelBy;

    // 时间线
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime cancelledAt;
    @TableLogic(value = "'1970-01-01 00:00:00'", delval = "now()")
    private LocalDateTime deleteAt;

}
