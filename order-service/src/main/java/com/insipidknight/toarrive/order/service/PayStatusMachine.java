package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.order.entity.enums.PayStatus;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

import static com.insipidknight.toarrive.order.entity.enums.PayStatus.*;

/**
 * 支付状态流转表。与订单状态机一致：这里只做读模型上的防御性校验，
 * 真正的并发保护由 mapper 中带状态条件的原子 UPDATE 完成。
 */
public class PayStatusMachine {

    private static final Map<PayStatus, Set<PayStatus>> TRANSITIONS = Map.of(
            UNPAID, EnumSet.of(PAYING),
            PAYING, EnumSet.of(PAID, UNPAID),
            PAID, EnumSet.noneOf(PayStatus.class)
    );

    public static boolean canTransfer(PayStatus from, PayStatus target) {
        return TRANSITIONS.getOrDefault(from, Set.of()).contains(target);
    }

    public static void assertTransfer(PayStatus from, PayStatus target) {
        if (!canTransfer(from, target)) {
            throw new BusinessException("支付状态不允许从【" + from.getDesc()
                    + "】变更为【" + target.getDesc() + "】");
        }
    }
}
