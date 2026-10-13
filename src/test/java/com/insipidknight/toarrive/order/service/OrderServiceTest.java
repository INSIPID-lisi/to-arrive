package com.insipidknight.toarrive.order.service;

import com.insipidknight.toarrive.common.ws.PushService;
import com.insipidknight.toarrive.common.ws.WsMessage;
import com.insipidknight.toarrive.order.entity.Order;
import com.insipidknight.toarrive.order.entity.enums.OrderStatus;
import com.insipidknight.toarrive.order.mapper.OrderMapper;
import com.insipidknight.toarrive.order.pricing.PricingService;
import com.insipidknight.toarrive.user.service.DriverService;
import com.insipidknight.toarrive.user.service.UserProfileService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderMapper orderMapper;

    @Mock
    private UserProfileService userProfileService;

    @Mock
    private DriverService driverService;

    @Mock
    private PricingService pricingService;

    @Mock
    private PushService pushService;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("超时取消：仅对抢到取消权（影响 1 行）的订单推 WS 给乘客")
    void cancelTimeoutOrders_pushOnlyForWonOrders() {
        Order won = cancelled("TA-A", 1L, 11L);
        Order lost = pendingOrder("TA-B", 2L, 22L);
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);

        when(orderMapper.selectTimeoutPending(eq(cutoff), anyInt())).thenReturn(List.of(won, lost));
        when(orderMapper.cancelBySystem(1L)).thenReturn(1);
        when(orderMapper.cancelBySystem(2L)).thenReturn(0);
        when(orderMapper.selectByOrderNo("TA-A")).thenReturn(won);

        orderService.cancelTimeoutOrders(cutoff);

        ArgumentCaptor<WsMessage> captor = ArgumentCaptor.forClass(WsMessage.class);
        verify(pushService, times(1)).sendToUser(eq(11L), captor.capture());
        assertThat(captor.getValue().type()).isEqualTo("ORDER_CANCELLED");
        assertThat(captor.getValue().orderNo()).isEqualTo("TA-A");
        assertThat(captor.getValue().status()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("超时取消：无候选订单时不推送")
    void cancelTimeoutOrders_nothingToDo() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(10);
        when(orderMapper.selectTimeoutPending(eq(cutoff), anyInt())).thenReturn(List.of());

        orderService.cancelTimeoutOrders(cutoff);

        verify(pushService, never()).sendToUser(any(), any());
    }

    private static Order pendingOrder(String orderNo, Long id, Long passengerId) {
        Order order = new Order();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setPassengerId(passengerId);
        order.setStatus(OrderStatus.PENDING);
        return order;
    }

    /** 取消后从库里重查到的形态（status=CANCELLED）。 */
    private static Order cancelled(String orderNo, Long id, Long passengerId) {
        Order order = pendingOrder(orderNo, id, passengerId);
        order.setStatus(OrderStatus.CANCELLED);
        return order;
    }
}
