package com.insipidknight.toarrive.order.vo;

import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderCreateVO {
    private String orderNo;
    private OrderStatus status;
    private String statusDesc;
    private BigDecimal estimatePrice;
    private LocalDateTime createdAt;

    public OrderCreateVO(Order order){
        this.orderNo = order.getOrderNo();
        this.status = order.getStatus();
        this.statusDesc =order.getStatus().getDesc();
        this.estimatePrice = order.getEstimatePrice();
        this.createdAt = order.getCreatedAt();
    }
}
