package com.insipidknight.toarrive.payment.service;

import com.insipidknight.toarrive.api.dto.PaymentRequest;

/**
 * 支付网关扩展点：当前仅站内钱包实现，后续可替换/新增真实网关实现。
 */
public interface PaymentGateway {

    /**
     * 完成订单结算（扣乘客、司机入账、平台抽成）。余额不足等失败情形抛 {@code BusinessException}。
     */
    void pay(PaymentRequest request);
}
