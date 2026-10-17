package com.insipidknight.toarrive.order.pricing;

import java.math.BigDecimal;

/**
 * 计价结果：distance 单位公里（保留 2 位），durationMinutes 单位分钟，price 单位元（保留 2 位）。
 */
public record FareResult(BigDecimal distance, int durationMinutes, BigDecimal price) {
}
