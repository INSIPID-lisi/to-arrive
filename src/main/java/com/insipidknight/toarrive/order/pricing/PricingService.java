package com.insipidknight.toarrive.order.pricing;

import com.insipidknight.toarrive.common.util.CalculateUtil;
import com.insipidknight.toarrive.order.entity.Order;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.LocalDateTime;

/**
 * 计价：起步价（含前 includedKm 公里）+ 超出里程费 + 时长费。
 * 距离当前取起终点直线距离，后续可替换为轨迹里程；规则与参数都收敛在此，便于替换。
 */
@Service
@RequiredArgsConstructor
public class PricingService {

    private static final int MIN_DURATION_MINUTES = 1;
    private static final int DISTANCE_SCALE = 2;
    private static final int PRICE_SCALE = 2;
    private static final BigDecimal MINUTES_PER_HOUR = BigDecimal.valueOf(60);

    private final PricingProperties properties;

    /** 下单预估：时长按平均车速推算 */
    public FareResult estimate(double startLng, double startLat, double endLng, double endLat) {
        BigDecimal distance = roundDistance(
                CalculateUtil.calculateDistance(startLng, startLat, endLng, endLat));
        int durationMinutes = estimateDurationMinutes(distance);
        return new FareResult(distance, durationMinutes, fare(distance, durationMinutes));
    }

    /** 关单结算：时长取 startedAt → finishedAt 的实际分钟数 */
    public FareResult settle(Order order, LocalDateTime finishedAt) {
        BigDecimal distance = roundDistance(CalculateUtil.calculateDistance(
                order.getStartLng(), order.getStartLat(), order.getEndLng(), order.getEndLat()));
        int durationMinutes = actualDurationMinutes(order.getStartedAt(), finishedAt);
        return new FareResult(distance, durationMinutes, fare(distance, durationMinutes));
    }

    private BigDecimal fare(BigDecimal distanceKm, int durationMinutes) {
        BigDecimal overKm = distanceKm.subtract(properties.getIncludedKm()).max(BigDecimal.ZERO);
        return properties.getBaseFare()
                .add(overKm.multiply(properties.getPerKm()))
                .add(BigDecimal.valueOf(durationMinutes).multiply(properties.getPerMinute()))
                .setScale(PRICE_SCALE, RoundingMode.HALF_UP);
    }

    private int estimateDurationMinutes(BigDecimal distanceKm) {
        BigDecimal minutes = distanceKm
                .divide(properties.getAssumedSpeedKmh(), 4, RoundingMode.HALF_UP)
                .multiply(MINUTES_PER_HOUR);
        return atLeastOneMinute(minutes.setScale(0, RoundingMode.HALF_UP).intValue());
    }

    private int actualDurationMinutes(LocalDateTime startedAt, LocalDateTime finishedAt) {
        if (startedAt == null || finishedAt == null) {
            return MIN_DURATION_MINUTES;
        }
        return atLeastOneMinute((int) Duration.between(startedAt, finishedAt).toMinutes());
    }

    private int atLeastOneMinute(int minutes) {
        return Math.max(minutes, MIN_DURATION_MINUTES);
    }

    private BigDecimal roundDistance(BigDecimal distanceKm) {
        return distanceKm.setScale(DISTANCE_SCALE, RoundingMode.HALF_UP);
    }
}
