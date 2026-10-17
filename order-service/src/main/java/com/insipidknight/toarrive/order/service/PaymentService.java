package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.api.client.PaymentClient;
import com.insipidknight.toarrive.api.client.PushClient;
import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.WsMessage;
import com.insipidknight.toarrive.api.util.ClientResults;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.entity.enums.PayStatus;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;
    private final PushClient pushClient;

    /**
     * 订单支付：先原子抢占「支付权」（markPaid），再扣款；两步同一事务，
     * 扣款失败（如余额不足）会整体回滚，天然幂等且不会重复扣款。
     */
    @Transactional
    public void payOrder(String orderNo, Long userId) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        if (!userId.equals(order.getPassengerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您无权支付该订单");
        }
        if (order.getStatus() != OrderStatus.FINISHED) {
            throw new BusinessException("订单未完成，无法支付");
        }
        if (order.getPayStatus() == PayStatus.PAID) {
            throw new BusinessException("订单已支付，请勿重复支付");
        }
        if (order.getActualPrice() == null) {
            throw new BusinessException("订单金额尚未结算，无法支付");
        }
        if (orderMapper.markPaid(order.getId(), userId) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }
        ClientResults.unwrap(paymentClient.pay(new PaymentRequest(userId, orderNo, order.getActualPrice())));

        Order paid = orderMapper.selectByOrderNo(orderNo);
        try {
            ClientResults.unwrap(pushClient.sendToUser(paid.getDriverId(),
                    new WsMessage("ORDER_PAID", orderNo, paid.getStatus().name(), paid.getActualPrice())));
        } catch (Exception e) {
            log.warn("订单支付推送失败 orderNo={}: {}", orderNo, e.getMessage());
        }
    }
}
