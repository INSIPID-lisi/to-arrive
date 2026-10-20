package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.api.client.PaymentClient;
import com.insipidknight.toarrive.api.client.PushClient;
import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.PaymentStatusDTO;
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

import java.time.LocalDateTime;

/**
 * 支付编排：不在数据库事务里包远程调用。
 * 1) 原子抢占 PAYING；2) 调用 payment-service 结算（按 order_no 幂等）；
 * 3) 成功置 PAID，明确失败回滚 UNPAID，结果未知则保持 PAYING 交给补偿任务。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PaymentService {

    private static final int RECONCILE_BATCH_LIMIT = 100;
    private static final long RECONCILE_GRACE_MINUTES = 1;
    private static final long PAYING_TIMEOUT_MINUTES = 10;

    private final OrderMapper orderMapper;
    private final PaymentClient paymentClient;
    private final PushClient pushClient;

    public void payOrder(String orderNo, Long userId) {
        Order order = requireOrder(orderNo);
        requirePayable(order, userId);
        PayStatusMachine.assertTransfer(order.getPayStatus(), PayStatus.PAYING);

        if (orderMapper.markPaying(order.getId()) == 0) {
            throw new BusinessException("订单状态已变化，请刷新后重试");
        }

        try {
            ClientResults.unwrap(paymentClient.pay(
                    new PaymentRequest(userId, order.getDriverId(), orderNo, order.getActualPrice())));
        } catch (BusinessException e) {
            // 余额不足等明确失败：释放支付权，允许用户充值后重试
            orderMapper.revertPaying(order.getId());
            throw e;
        } catch (Exception e) {
            // 超时/网络异常属于结果未知，保持 PAYING，由补偿任务查询真实结果
            log.warn("支付结果未知，等待补偿确认 orderNo={}: {}", orderNo, e.getMessage());
            return;
        }
        confirmPaid(orderNo);
    }

    /**
     * 扫描停留超过补偿水位的 PAYING 订单，向 payment-service 查询最终结果。
     */
    public void reconcilePayingOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(RECONCILE_GRACE_MINUTES);
        for (Order order : orderMapper.selectPayingBefore(cutoff, RECONCILE_BATCH_LIMIT)) {
            try {
                reconcile(order);
            } catch (Exception e) {
                log.warn("支付补偿失败 orderNo={}: {}", order.getOrderNo(), e.getMessage());
            }
        }
    }

    private void reconcile(Order order) {
        PaymentStatusDTO status = ClientResults.unwrap(paymentClient.status(order.getOrderNo()));
        if (status != null && status.isSuccess()) {
            confirmPaid(order.getOrderNo());
            return;
        }

        LocalDateTime timeoutAt = LocalDateTime.now().minusMinutes(PAYING_TIMEOUT_MINUTES);
        if (order.getPayingAt() != null && order.getPayingAt().isBefore(timeoutAt)) {
            if (orderMapper.revertPaying(order.getId()) == 1) {
                log.warn("支付长时间未确认，回滚为未支付 orderNo={}", order.getOrderNo());
            }
        }
    }

    private void confirmPaid(String orderNo) {
        Order order = requireOrder(orderNo);
        if (orderMapper.confirmPaid(order.getId()) == 0) {
            return;
        }

        Order paid = requireOrder(orderNo);
        try {
            ClientResults.unwrap(pushClient.sendToUser(paid.getDriverId(),
                    new WsMessage("ORDER_PAID", orderNo, paid.getStatus().name(), paid.getActualPrice())));
        } catch (Exception e) {
            log.warn("订单支付推送失败 orderNo={}: {}", orderNo, e.getMessage());
        }
    }

    private Order requireOrder(String orderNo) {
        Order order = orderMapper.selectByOrderNo(orderNo);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    private void requirePayable(Order order, Long userId) {
        if (!userId.equals(order.getPassengerId())) {
            throw new BusinessException(ErrorCode.FORBIDDEN, "您无权支付该订单");
        }
        if (order.getStatus() != OrderStatus.FINISHED) {
            throw new BusinessException("订单未完成，无法支付");
        }
        if (order.getPayStatus() == PayStatus.PAID) {
            throw new BusinessException("订单已支付，请勿重复支付");
        }
        if (order.getPayStatus() == PayStatus.PAYING) {
            throw new BusinessException("支付处理中，请稍后查询");
        }
        if (order.getActualPrice() == null) {
            throw new BusinessException("订单金额尚未结算，无法支付");
        }
        if (order.getDriverId() == null) {
            throw new BusinessException("订单缺少司机信息，无法结算");
        }
    }
}
