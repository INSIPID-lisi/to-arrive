package com.insipidknight.toarrive.location.config;

import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRule;
import com.alibaba.csp.sentinel.slots.block.degrade.DegradeRuleManager;
import com.alibaba.csp.sentinel.slots.block.degrade.circuitbreaker.CircuitBreakerStrategy;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * 慢调用比例熔断：统计窗口内慢调用占比超过阈值且请求量达标时打开熔断器。
 * 规则随应用启动加载，可在 Sentinel 控制台查看并临时调整。
 */
@Slf4j
@Configuration
public class SentinelDegradeConfig {

    private static final int STAT_INTERVAL_MS = 10_000;

    @PostConstruct
    void loadRules() {
        List<DegradeRule> rules = List.of(
                slowRatio("location:report", 300, 0.5, 10));
        DegradeRuleManager.loadRules(rules);
        log.info("Sentinel 熔断规则已加载: {}", rules);
    }

    private DegradeRule slowRatio(String resource, double maxRtMs, double ratio, int timeWindowSeconds) {
        return new DegradeRule(resource)
                .setGrade(CircuitBreakerStrategy.SLOW_REQUEST_RATIO.getType())
                .setCount(maxRtMs)
                .setSlowRatioThreshold(ratio)
                .setMinRequestAmount(5)
                .setStatIntervalMs(STAT_INTERVAL_MS)
                .setTimeWindow(timeWindowSeconds);
    }
}
