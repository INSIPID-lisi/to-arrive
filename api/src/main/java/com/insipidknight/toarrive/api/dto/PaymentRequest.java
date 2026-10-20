package com.insipidknight.toarrive.api.dto;

import java.math.BigDecimal;

/**
 * 订单支付结算请求：从 payerId 扣 amount，按平台比例分账给 driverId。
 */
public record PaymentRequest(Long payerId, Long driverId, String orderNo, BigDecimal amount) {
}
