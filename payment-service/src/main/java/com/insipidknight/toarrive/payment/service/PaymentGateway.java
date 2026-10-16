package com.insipidknight.toarrive.payment.service;

import java.math.BigDecimal;

/**
 * 支付网关扩展点：当前仅站内钱包实现，后续可替换/新增真实网关实现。
 */
public interface PaymentGateway {

    /**
     * 从 payerId 扣款 amount 到订单 orderNo。余额不足等失败情形抛 {@code BusinessException}。
     */
    void pay(Long payerId, String orderNo, BigDecimal amount);
}
