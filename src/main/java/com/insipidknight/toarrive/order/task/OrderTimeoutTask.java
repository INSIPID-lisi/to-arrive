package com.insipidknight.toarrive.order.task;

import com.insipidknight.toarrive.order.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderTimeoutTask {

    private final OrderService orderService;

    @Value("${app.order.accept-timeout-minutes:10}")
    private long acceptTimeoutMinutes;

    /**
     * 每分钟扫描并取消长时间无人接单的订单（status=1 且超过窗口）。
     */
    @Scheduled(fixedDelay = 60_000)
    public void cancelTimeoutOrders() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(acceptTimeoutMinutes);
        orderService.cancelTimeoutOrders(cutoff);
    }
}
