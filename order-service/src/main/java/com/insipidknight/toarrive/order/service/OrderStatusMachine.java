package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.insipidknight.toarrive.order.entity.enums.OrderStatus.*;

public class OrderStatusMachine {

    private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = Map.of(
      PENDING, EnumSet.of(ACCEPTED,CANCELLED),
      ACCEPTED, EnumSet.of(ARRIVED,CANCELLED),
      ARRIVED,   EnumSet.of(IN_TRIP, CANCELLED),
      IN_TRIP,   EnumSet.of(FINISHED),
      CANCELLED,  EnumSet.noneOf(OrderStatus.class),
      FINISHED,  EnumSet.noneOf(OrderStatus.class)
    );

    public static boolean canTransfer(OrderStatus from,OrderStatus target){
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(target);
    }

    public static void assertTransfer(OrderStatus from, OrderStatus target) {
        if (!canTransfer(from, target)) {
            throw new BusinessException("订单状态不允许从【" + from.getDesc()
                    + "】变更为【" + target.getDesc() + "】");
        }
    }
}
