package com.insipidknight.toarrive.api.dto;

import java.math.BigDecimal;

/**
 * 订单支付扣款请求：从 payerId 扣 amount 到订单 orderNo。
 */
public record PaymentRequest(Long payerId, String orderNo, BigDecimal amount) {
}
