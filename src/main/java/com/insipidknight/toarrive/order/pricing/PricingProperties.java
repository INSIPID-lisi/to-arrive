package com.insipidknight.toarrive.order.pricing;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

/**
 * 计价参数，绑定 app.pricing.*。独立成类便于替换计价规则。
 */
@Data
@Component
@ConfigurationProperties(prefix = "app.pricing")
public class PricingProperties {

    /** 起步价（元），已包含 includedKm 公里 */
    private BigDecimal baseFare = new BigDecimal("8.00");

    /** 起步价包含的里程（公里） */
    private BigDecimal includedKm = new BigDecimal("3.00");

    /** 超出包含里程后每公里单价（元） */
    private BigDecimal perKm = new BigDecimal("2.00");

    /** 每分钟时长费（元） */
    private BigDecimal perMinute = new BigDecimal("0.50");

    /** 预估时长用的平均车速（km/h） */
    private BigDecimal assumedSpeedKmh = new BigDecimal("30");
}
