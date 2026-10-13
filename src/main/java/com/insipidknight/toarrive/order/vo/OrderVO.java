package com.insipidknight.toarrive.order.vo;

import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.CancelBy;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.entity.enums.PayStatus;
import com.insipidknight.toarrive.user.vo.DriverBriefVO;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class OrderVO {
    private String orderNo;
    private OrderStatus status;
    private String statusDesc;

    // 起终点
    private Double startLng;
    private Double startLat;
    private String startAddress;
    private Double endLng;
    private Double endLat;
    private String endAddress;

    // 行程
    private BigDecimal distance;
    private Integer duration;

    // 计费
    private BigDecimal estimatePrice;
    private BigDecimal actualPrice;

    // 支付
    private PayStatus payStatus;
    private String payStatusDesc;
    private LocalDateTime paidAt;

    // 司机信息（接单后才有）
    private DriverBriefVO driverInfo;

    // 取消信息
    private String cancelReason;
    private CancelBy cancelBy;

    // 时间线
    private LocalDateTime createdAt;
    private LocalDateTime acceptedAt;
    private LocalDateTime arrivedAt;
    private LocalDateTime startedAt;
    private LocalDateTime finishedAt;
    private LocalDateTime cancelledAt;

    public OrderVO(Order order) {
        this.orderNo = order.getOrderNo();
        this.status = order.getStatus();
        this.statusDesc = order.getStatus().getDesc();
        this.startLng = order.getStartLng();
        this.startLat = order.getStartLat();
        this.startAddress = order.getStartAddress();
        this.endLng = order.getEndLng();
        this.endLat = order.getEndLat();
        this.endAddress = order.getEndAddress();
        this.distance = order.getDistance();
        this.duration = order.getDuration();
        this.estimatePrice = order.getEstimatePrice();
        this.actualPrice = order.getActualPrice();
        this.payStatus = order.getPayStatus();
        this.payStatusDesc = order.getPayStatus() == null ? null : order.getPayStatus().getDesc();
        this.paidAt = order.getPaidAt();
        this.cancelReason = order.getCancelReason();
        this.cancelBy = order.getCancelBy();
        this.createdAt = order.getCreatedAt();
        this.acceptedAt = order.getAcceptedAt();
        this.arrivedAt = order.getArrivedAt();
        this.startedAt = order.getStartedAt();
        this.finishedAt = order.getFinishedAt();
        this.cancelledAt = order.getCancelledAt();
    }
}
