package com.insipidknight.toarrive.api.dto;

import java.math.BigDecimal;

/**
 * 支付结算状态：status 与 payment-service 的 PaymentOrderStatus 编码一致。
 */
public record PaymentStatusDTO(String orderNo, Integer status,
                               BigDecimal amount, BigDecimal commission, BigDecimal driverIncome) {

    public static final int SUCCESS = 2;

    public boolean isSuccess() {
        return status != null && status == SUCCESS;
    }
}
