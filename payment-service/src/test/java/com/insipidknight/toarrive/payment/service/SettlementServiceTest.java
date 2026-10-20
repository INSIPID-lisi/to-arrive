package com.insipidknight.toarrive.payment.service;

import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.PaymentStatusDTO;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.payment.entity.PaymentOrder;
import com.insipidknight.toarrive.payment.entity.enums.PaymentOrderStatus;
import com.insipidknight.toarrive.payment.entity.enums.TxType;
import com.insipidknight.toarrive.payment.mapper.PaymentOrderMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class SettlementServiceTest {

    private static final String ORDER_NO = "ORDER_20261020_0001";
    private static final Long PASSENGER_ID = 1001L;
    private static final Long DRIVER_ID = 2002L;
    private static final Long PLATFORM_ID = 0L;
    private static final BigDecimal COMMISSION_RATE = new BigDecimal("0.20");

    private PaymentOrderMapper paymentOrderMapper;
    private WalletService walletService;
    private SettlementService settlementService;

    @BeforeEach
    void setUp() {
        paymentOrderMapper = mock(PaymentOrderMapper.class);
        walletService = mock(WalletService.class);
        settlementService = new SettlementService(
                paymentOrderMapper, walletService, noopTransactionManager(), COMMISSION_RATE);
    }

    @Test
    @DisplayName("结算：扣乘客、给司机入账、记平台抽成，并落成功状态")
    void settleSplitsAmount() {
        when(paymentOrderMapper.selectById(ORDER_NO)).thenReturn(null);

        settlementService.settle(
                new PaymentRequest(PASSENGER_ID, DRIVER_ID, ORDER_NO, new BigDecimal("100.00")));

        ArgumentCaptor<PaymentOrder> captor = ArgumentCaptor.forClass(PaymentOrder.class);
        verify(paymentOrderMapper).insert(captor.capture());
        PaymentOrder inserted = captor.getValue();
        assertThat(inserted.getOrderNo()).isEqualTo(ORDER_NO);
        assertThat(inserted.getAmount()).isEqualByComparingTo("100.00");
        assertThat(inserted.getCommission()).isEqualByComparingTo("20.00");
        assertThat(inserted.getDriverIncome()).isEqualByComparingTo("80.00");

        verify(walletService).deduct(PASSENGER_ID, new BigDecimal("100.00"), ORDER_NO);
        verify(walletService).credit(
                DRIVER_ID, new BigDecimal("80.00"), TxType.DRIVER_INCOME, ORDER_NO, "订单收入");
        verify(walletService).credit(
                PLATFORM_ID, new BigDecimal("20.00"), TxType.COMMISSION, ORDER_NO, "平台抽成");
        verify(paymentOrderMapper).updateById(argThat(
                (PaymentOrder order) -> order.getStatus() == PaymentOrderStatus.SUCCESS));
    }

    @Test
    @DisplayName("结算：抽成按分四舍五入，司机收入 = 金额 - 抽成")
    void settleRoundsCommissionToCent() {
        when(paymentOrderMapper.selectById(ORDER_NO)).thenReturn(null);

        settlementService.settle(
                new PaymentRequest(PASSENGER_ID, DRIVER_ID, ORDER_NO, new BigDecimal("0.99")));

        verify(walletService).credit(
                DRIVER_ID, new BigDecimal("0.79"), TxType.DRIVER_INCOME, ORDER_NO, "订单收入");
        verify(walletService).credit(
                PLATFORM_ID, new BigDecimal("0.20"), TxType.COMMISSION, ORDER_NO, "平台抽成");
    }

    @Test
    @DisplayName("结算：已成功的订单号直接返回，不重复动账")
    void settleIsIdempotentForSucceededOrder() {
        PaymentOrder existing = new PaymentOrder();
        existing.setOrderNo(ORDER_NO);
        existing.setStatus(PaymentOrderStatus.SUCCESS);
        when(paymentOrderMapper.selectById(ORDER_NO)).thenReturn(existing);

        settlementService.settle(
                new PaymentRequest(PASSENGER_ID, DRIVER_ID, ORDER_NO, new BigDecimal("100.00")));

        verifyNoInteractions(walletService);
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("结算：金额非法时拒绝，不产生任何资金变动")
    void settleRejectsInvalidAmount() {
        assertThatThrownBy(() -> settlementService.settle(
                new PaymentRequest(PASSENGER_ID, DRIVER_ID, ORDER_NO, BigDecimal.ZERO)))
                .isInstanceOf(BusinessException.class);

        verifyNoInteractions(walletService);
        verify(paymentOrderMapper, never()).insert(any(PaymentOrder.class));
    }

    @Test
    @DisplayName("状态查询：映射订单号、金额、抽成与司机收入")
    void getStatusMapsFields() {
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(ORDER_NO);
        order.setStatus(PaymentOrderStatus.SUCCESS);
        order.setAmount(new BigDecimal("100.00"));
        order.setCommission(new BigDecimal("20.00"));
        order.setDriverIncome(new BigDecimal("80.00"));
        when(paymentOrderMapper.selectById(ORDER_NO)).thenReturn(order);

        PaymentStatusDTO status = settlementService.getStatus(ORDER_NO);

        assertThat(status.isSuccess()).isTrue();
        assertThat(status.orderNo()).isEqualTo(ORDER_NO);
        assertThat(status.amount()).isEqualByComparingTo("100.00");
        assertThat(status.commission()).isEqualByComparingTo("20.00");
        assertThat(status.driverIncome()).isEqualByComparingTo("80.00");
    }

    private static PlatformTransactionManager noopTransactionManager() {
        return new PlatformTransactionManager() {
            @Override
            public TransactionStatus getTransaction(TransactionDefinition definition) {
                return new SimpleTransactionStatus();
            }

            @Override
            public void commit(TransactionStatus status) {
            }

            @Override
            public void rollback(TransactionStatus status) {
            }
        };
    }
}
