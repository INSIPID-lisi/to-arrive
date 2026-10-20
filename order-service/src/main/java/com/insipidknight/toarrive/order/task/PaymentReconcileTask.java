package com.insipidknight.toarrive.order.task;

import com.insipidknight.toarrive.order.service.PaymentService;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 支付补偿：确认「远程已扣款但本地未落 PAID」的订单，避免资金与订单状态不一致。
 */
@Component
@RequiredArgsConstructor
public class PaymentReconcileTask {

    private final PaymentService paymentService;

    @Scheduled(fixedDelay = 60_000)
    public void reconcilePayingOrders() {
        paymentService.reconcilePayingOrders();
    }
}
