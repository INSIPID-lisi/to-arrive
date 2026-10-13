package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.common.ws.PushService;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.entity.enums.PayStatus;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import com.insipidknight.toarrive.wallet.service.PaymentGateway;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    private static final Long PASSENGER_ID = 1L;
    private static final Long DRIVER_ID = 2L;
    private static final Long ORDER_ID = 10L;
    private static final String ORDER_NO = "TA202601011200000001";

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private PaymentGateway paymentGateway;

    @Mock
    private PushService pushService;

    @InjectMocks
    private PaymentService paymentService;

    @Test
    @DisplayName("支付：订单不存在 → 报错，不触碰网关")
    void orderNotFound() {
        when(orderMapper.selectByOrderNo(ORDER_NO)).thenReturn(null);

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, PASSENGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单不存在");

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("支付：非本人订单 → 报错，不触碰网关")
    void notOwner() {
        when(orderMapper.selectByOrderNo(ORDER_NO))
                .thenReturn(order(OrderStatus.FINISHED, PayStatus.UNPAID, "20.00", 999L));

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, PASSENGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("您无权支付该订单");

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("支付：订单未完成 → 报错，不触碰网关")
    void notFinished() {
        when(orderMapper.selectByOrderNo(ORDER_NO))
                .thenReturn(order(OrderStatus.IN_TRIP, PayStatus.UNPAID, "20.00", PASSENGER_ID));

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, PASSENGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单未完成，无法支付");

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("支付：已支付 → 报错，不触碰网关")
    void alreadyPaid() {
        when(orderMapper.selectByOrderNo(ORDER_NO))
                .thenReturn(order(OrderStatus.FINISHED, PayStatus.PAID, "20.00", PASSENGER_ID));

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, PASSENGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单已支付，请勿重复支付");

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("支付：金额未结算 → 报错，不触碰网关")
    void notSettled() {
        when(orderMapper.selectByOrderNo(ORDER_NO))
                .thenReturn(order(OrderStatus.FINISHED, PayStatus.UNPAID, null, PASSENGER_ID));

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, PASSENGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单金额尚未结算，无法支付");

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("支付：markPaid 0 行（并发已被处理）→ 报错且不扣款")
    void markPaidLostRace() {
        when(orderMapper.selectByOrderNo(ORDER_NO))
                .thenReturn(order(OrderStatus.FINISHED, PayStatus.UNPAID, "20.00", PASSENGER_ID));
        when(orderMapper.markPaid(ORDER_ID, PASSENGER_ID)).thenReturn(0);

        assertThatThrownBy(() -> paymentService.payOrder(ORDER_NO, PASSENGER_ID))
                .isInstanceOf(BusinessException.class)
                .hasMessage("订单状态已变化，请刷新后重试");

        verifyNoInteractions(paymentGateway);
    }

    @Test
    @DisplayName("支付成功：标记已支付后按实付金额扣款")
    void success() {
        when(orderMapper.selectByOrderNo(ORDER_NO))
                .thenReturn(order(OrderStatus.FINISHED, PayStatus.UNPAID, "20.00", PASSENGER_ID));
        when(orderMapper.markPaid(ORDER_ID, PASSENGER_ID)).thenReturn(1);

        paymentService.payOrder(ORDER_NO, PASSENGER_ID);

        verify(orderMapper).markPaid(ORDER_ID, PASSENGER_ID);
        verify(paymentGateway).pay(PASSENGER_ID, ORDER_NO, new BigDecimal("20.00"));
        verify(pushService).sendToUser(eq(DRIVER_ID), any());
    }

    // ---------- helpers ----------

    private static Order order(OrderStatus status, PayStatus payStatus, String actualPrice, Long passengerId) {
        Order order = new Order();
        order.setId(ORDER_ID);
        order.setOrderNo(ORDER_NO);
        order.setPassengerId(passengerId);
        order.setDriverId(DRIVER_ID);
        order.setStatus(status);
        order.setPayStatus(payStatus);
        order.setActualPrice(actualPrice == null ? null : new BigDecimal(actualPrice));
        return order;
    }
}
