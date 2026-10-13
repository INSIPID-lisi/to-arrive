package com.insipidknight.toarrive.order.pricing;

import com.insipidknight.toarrive.common.util.CalculateUtil;
import com.insipidknight.toarrive.order.entity.Order;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;

class PricingServiceTest {

    private static final LocalDateTime STARTED_AT = LocalDateTime.of(2026, 1, 1, 10, 0);

    private PricingProperties properties;
    private PricingService pricingService;

    @BeforeEach
    void setUp() {
        properties = new PricingProperties();
        pricingService = new PricingService(properties);
    }

    // ---------- settle ----------

    @Test
    @DisplayName("结算：里程未超起步含里程 → 只收起步价 + 时长费")
    void settle_withinIncludedKm() {
        // 约 1.3km < 3km
        Order order = order(116.397, 39.908, 116.410, 39.915, STARTED_AT);

        FareResult fare = pricingService.settle(order, STARTED_AT.plusMinutes(10));

        assertThat(fare.distance()).isLessThan(properties.getIncludedKm());
        assertThat(fare.durationMinutes()).isEqualTo(10);
        assertThat(fare.price()).isEqualByComparingTo(new BigDecimal("13.00")); // 8.00 + 10 * 0.50
    }

    @Test
    @DisplayName("结算：超过起步含里程 → 叠加里程费")
    void settle_overIncludedKm() {
        Order order = order(116.397, 39.908, 116.517, 40.008, STARTED_AT);

        FareResult fare = pricingService.settle(order, STARTED_AT.plusMinutes(20));

        assertThat(fare.distance()).isGreaterThan(properties.getIncludedKm());
        assertThat(fare.durationMinutes()).isEqualTo(20);

        BigDecimal expected = properties.getBaseFare()
                .add(fare.distance().subtract(properties.getIncludedKm()).multiply(properties.getPerKm()))
                .add(BigDecimal.valueOf(fare.durationMinutes()).multiply(properties.getPerMinute()))
                .setScale(2, RoundingMode.HALF_UP);
        assertThat(fare.price()).isEqualByComparingTo(expected);
    }

    @Test
    @DisplayName("结算：行程不足 1 分钟按 1 分钟计")
    void settle_durationAtLeastOneMinute() {
        Order order = order(116.397, 39.908, 116.402, 39.910, STARTED_AT);

        FareResult fare = pricingService.settle(order, STARTED_AT.plusSeconds(20));

        assertThat(fare.durationMinutes()).isEqualTo(1);
        assertThat(fare.price()).isEqualByComparingTo(new BigDecimal("8.50")); // 8.00 + 1 * 0.50
    }

    @Test
    @DisplayName("结算：金额按 HALF_UP 保留 2 位")
    void settle_priceRounding() {
        properties.setBaseFare(new BigDecimal("1.005"));
        properties.setIncludedKm(new BigDecimal("100.00"));
        properties.setPerMinute(BigDecimal.ZERO);
        Order order = order(116.397, 39.908, 116.397, 39.908, STARTED_AT);

        FareResult fare = pricingService.settle(order, STARTED_AT);

        assertThat(fare.price()).isEqualByComparingTo(new BigDecimal("1.01"));
        assertThat(fare.price().scale()).isEqualTo(2);
    }

    // ---------- estimate ----------

    @Test
    @DisplayName("预估：距离取直线距离保留 2 位，价格与结算同一套公式")
    void estimate_usesStraightLineAndSameFormula() {
        FareResult fare = pricingService.estimate(116.397, 39.908, 116.407, 39.918);

        BigDecimal expectedDistance = CalculateUtil.calculateDistance(116.397, 39.908, 116.407, 39.918)
                .setScale(2, RoundingMode.HALF_UP);
        assertThat(fare.distance()).isEqualByComparingTo(expectedDistance);
        assertThat(fare.durationMinutes()).isGreaterThanOrEqualTo(1);

        BigDecimal expectedPrice = properties.getBaseFare()
                .add(fare.distance().subtract(properties.getIncludedKm()).max(BigDecimal.ZERO)
                        .multiply(properties.getPerKm()))
                .add(BigDecimal.valueOf(fare.durationMinutes()).multiply(properties.getPerMinute()))
                .setScale(2, RoundingMode.HALF_UP);
        assertThat(fare.price()).isEqualByComparingTo(expectedPrice);
        assertThat(fare.price().scale()).isEqualTo(2);
    }

    @Test
    @DisplayName("预估：起终点相同 → 里程 0，时长下限 1 分钟")
    void estimate_zeroDistance() {
        FareResult fare = pricingService.estimate(116.397, 39.908, 116.397, 39.908);

        assertThat(fare.distance()).isEqualByComparingTo(new BigDecimal("0.00"));
        assertThat(fare.durationMinutes()).isEqualTo(1);
    }

    // ---------- helpers ----------

    private static Order order(double startLng, double startLat,
                               double endLng, double endLat, LocalDateTime startedAt) {
        Order order = new Order();
        order.setStartLng(startLng);
        order.setStartLat(startLat);
        order.setEndLng(endLng);
        order.setEndLat(endLat);
        order.setStartedAt(startedAt);
        return order;
    }
}
