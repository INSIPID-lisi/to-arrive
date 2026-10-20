package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.api.client.PaymentClient;
import com.insipidknight.toarrive.api.client.PushClient;
import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.PaymentStatusDTO;
import com.insipidknight.toarrive.api.dto.WsMessage;
import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.entity.enums.PayStatus;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentServiceTest {

    private static final Long ORDER_ID = 9001L;
    private static final String ORDER_NO = "ORDER_20261020_0001";
    private static final Long PASSENGER_ID = 1001L;
    private static final Long DRIVER_ID = 2002L;
    private static final BigDecimal ACTUAL_PRICE = new BigDecimal("50.00");

    private OrderMapper orderMapper;
    private PaymentClient paymentClient;
    private PushClient pushClient;
    private PaymentService paymentService;

    @BeforeEach
    void setUp() {
        orderMapper = mock(OrderMapper.class);
        paymentClient = mock(PaymentClient.class);
        pushClient = mock(PushClient.class);
        paymentService = new PaymentService(orderMapper, paymentClient, pushClient);
    }

    @Test
    @DisplayName("支付：抢占 PAYING 成功且远程结算成功 → 置 PAID 并推送司机")
    void payOrderConfirmsPaidOnRemoteSuccess() {
        Order order = finishedOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.markPaying(ORDER_ID)).thenReturn(1);
        when(paymentClient.pay(any())).thenReturn(Result.success(null));
        when(orderMapper.confirmPaid(ORDER_ID)).thenReturn(1);
        when(pushClient.sendToUser(anyLong(), any())).thenReturn(Result.success(null));

        paymentService.payOrder(ORDER_NO, PASSENGER_ID);

        verify(paymentClient).pay(new PaymentRequest(PASSENGER_ID, DRIVER_ID, ORDER_NO, ACTUAL_PRICE));
        verify(orderMapper).confirmPaid(ORDER_ID);
        verify(pushClient).sendToUser(eq(DRIVER_ID),
                argThat((WsMessage message) -> "ORDER_PAID".equals(message.type())));
    }

    @Test
    @DisplayName("支付：远程明确失败（如余额不足）→ 回滚 PAYING 并抛出原异常")
    void payOrderRevertsOnRemoteBusinessFailure() {
        Order order = finishedOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.markPaying(ORDER_ID)).thenReturn(1);
        when(paymentClient.pay(any())).thenThrow(new BusinessException("余额不足"));

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, PASSENGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("余额不足");

        verify(orderMapper).revertPaying(ORDER_ID);
        verify(orderMapper, never()).confirmPaid(ORDER_ID);
    }

    @Test
    @DisplayName("支付：网络/超时等结果未知 → 保持 PAYING，交给补偿任务确认")
    void payOrderKeepsPayingOnUnknownResult() {
        Order order = finishedOrder();
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.markPaying(ORDER_ID)).thenReturn(1);
        when(paymentClient.pay(any())).thenThrow(new RuntimeException("timeout"));

        paymentService.payOrder(ORDER_NO, PASSENGER_ID);

        verify(orderMapper, never()).revertPaying(ORDER_ID);
        verify(orderMapper, never()).confirmPaid(ORDER_ID);
    }

    @Test
    @DisplayName("支付：非订单乘客支付被拒绝")
    void payOrderRejectsNonOwner() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(finishedOrder());

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, 9999L))
                .isInstanceOf(BusinessException.class);

        verify(paymentClient, never()).pay(any());
    }

    @Test
    @DisplayName("补偿：远程已成功 → 本地置 PAID")
    void reconcileConfirmsPaidWhenRemoteSucceeded() {
        Order order = payingOrder(LocalDateTime.now().minusMinutes(2));
        when(orderMapper.selectPayingBefore(any(), anyInt())).thenReturn(List.of(order));
        when(paymentClient.status(ORDER_NO)).thenReturn(Result.success(
                new PaymentStatusDTO(ORDER_NO, PaymentStatusDTO.SUCCESS, ACTUAL_PRICE,
                        new BigDecimal("10.00"), new BigDecimal("40.00"))));
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(order);
        when(orderMapper.confirmPaid(ORDER_ID)).thenReturn(1);
        when(pushClient.sendToUser(anyLong(), any())).thenReturn(Result.success(null));

        paymentService.reconcilePayingOrders();

        verify(orderMapper).confirmPaid(ORDER_ID);
    }

    @Test
    @DisplayName("补偿：远程仍无结果且超过超时水位 → 回滚为未支付，允许重新支付")
    void reconcileRevertsTimedOutPayingOrder() {
        Order order = payingOrder(LocalDateTime.now().minusMinutes(11));
        when(orderMapper.selectPayingBefore(any(), anyInt())).thenReturn(List.of(order));
        when(paymentClient.status(ORDER_NO)).thenReturn(Result.success(null));
        when(orderMapper.revertPaying(ORDER_ID)).thenReturn(1);

        paymentService.reconcilePayingOrders();

        verify(orderMapper).revertPaying(ORDER_ID);
        verify(orderMapper, never()).confirmPaid(ORDER_ID);
    }

    private static Order finishedOrder() {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setPassengerId(PASSENGER_ID);
        order.setDriverId(DRIVER_ID);
        order.setStatus(OrderStatus.FINISHED);
        order.setPayStatus(PayStatus.UNPAID);
        order.setActualPrice(ACTUAL_PRICE);
        return order;
    }

    private static Order payingOrder(LocalDateTime payingAt) {
        Order order = finishedOrder();
        order.setPayStatus(PayStatus.PAYING);
        order.setPayingAt(payingAt);
        return order;
    }
}
