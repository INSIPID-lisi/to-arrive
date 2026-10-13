package com.insipidknight.toarrive.order.vo;

import com.insipidknight.toarrive.order.entity.Order;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 抢单池订单：在订单快照基础上附带「司机当前位置到订单起点」的距离。
 * 与之相对，{@link OrderVO#getDistance()} 是本单的行程距离，语义不同。
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class PendingOrderVO extends OrderVO {

    /** 司机当前位置到订单起点的直线距离（米） */
    private Double pickupDistanceMeters;

    public PendingOrderVO(Order order, Double pickupDistanceMeters) {
        super(order);
        this.pickupDistanceMeters = pickupDistanceMeters;
    }
}
