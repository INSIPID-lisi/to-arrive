package com.insipidknight.toarrive.payment.service;

import com.insipidknight.toarrive.api.dto.PaymentRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 站内钱包支付实现：委派 {@link SettlementService} 完成扣款与分账。
 */
@Component
@RequiredArgsConstructor
public class MockWalletPaymentGateway implements PaymentGateway {

    private final SettlementService settlementService;

    @Override
    public void pay(PaymentRequest request) {
        settlementService.settle(request);
    }
}
