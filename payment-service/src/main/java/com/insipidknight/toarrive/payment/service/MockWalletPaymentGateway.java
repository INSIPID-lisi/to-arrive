package com.insipidknight.toarrive.payment.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 站内钱包支付实现：直接委派 {@link WalletService#deduct} 完成扣款。
 */
@Component
@RequiredArgsConstructor
public class MockWalletPaymentGateway implements PaymentGateway {

    private final WalletService walletService;

    @Override
    public void pay(Long payerId, String orderNo, BigDecimal amount) {
        walletService.deduct(payerId, amount, orderNo);
    }
}
