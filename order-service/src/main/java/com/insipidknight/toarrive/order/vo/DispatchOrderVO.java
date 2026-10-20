package com.insipidknight.toarrive.order.vo;

import com.insipidknight.toarrive.order.entity.Order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 派单推送载荷：给司机展示订单起终点、预估价，以及司机到起点的接驾距离。
 */
public record DispatchOrderVO(String orderNo,
                              Double startLng, Double startLat, String startAddress,
                              Double endLng, Double endLat, String endAddress,
                              BigDecimal estimatePrice,
                              Double pickupDistanceMeters,
                              LocalDateTime createdAt) {

    public DispatchOrderVO(Order order, Double pickupDistanceMeters) {
        this(order.getOrderNo(),
                order.getStartLng(), order.getStartLat(), order.getStartAddress(),
                order.getEndLng(), order.getEndLat(), order.getEndAddress(),
                order.getEstimatePrice(), pickupDistanceMeters, order.getCreatedAt());
    }
}
