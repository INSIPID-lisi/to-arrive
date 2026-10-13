package com.insipidknight.toarrive.wallet;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.wallet.entity.WalletTransaction;
import com.insipidknight.toarrive.wallet.mapper.WalletMapper;
import com.insipidknight.toarrive.wallet.mapper.WalletTransactionMapper;
import com.insipidknight.toarrive.wallet.service.WalletService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 钱包并发扣款验证：验证「原子条件 UPDATE」在真实 MySQL 下不会超扣。
 * 有意不加 @Transactional —— 需要多线程各自提交事务，故在 @AfterEach 手工清库。
 */
@SpringBootTest
class WalletConcurrencyTest {

    private static final Long USER_ID = 999_999_999_001L;
    private static final int THREADS = 20;
    private static final BigDecimal START_BALANCE = new BigDecimal("100.00");
    private static final BigDecimal EACH = new BigDecimal("10.00");

    @Autowired
    private WalletService walletService;

    @Autowired
    private WalletMapper walletMapper;

    @Autowired
    private WalletTransactionMapper walletTransactionMapper;

    @AfterEach
    void cleanup() {
        walletMapper.deleteById(USER_ID);
        walletTransactionMapper.delete(new LambdaQueryWrapper<WalletTransaction>()
                .eq(WalletTransaction::getUserId, USER_ID));
    }

    @Test
    @DisplayName("余额 100，20 线程各扣 10 → 恰好 10 次成功，终值 0，绝不出现负余额")
    void concurrentDeduct_neverOverdraws() throws Exception {
        walletService.recharge(USER_ID, START_BALANCE);

        CountDownLatch ready = new CountDownLatch(THREADS);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger success = new AtomicInteger();
        ExecutorService pool = Executors.newFixedThreadPool(THREADS);
        List<Future<?>> futures = new ArrayList<>();
        try {
            for (int i = 0; i < THREADS; i++) {
                String orderNo = "TA_CONC_" + i;
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    try {
                        start.await();
                        walletService.deduct(USER_ID, EACH, orderNo);
                        success.incrementAndGet();
                    } catch (BusinessException e) {
                        // 余额不足，预期内的失败分支
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }));
            }
            assertThat(ready.await(10, TimeUnit.SECONDS)).isTrue();
            start.countDown();
            for (Future<?> future : futures) {
                future.get(30, TimeUnit.SECONDS);
            }
        } finally {
            pool.shutdownNow();
        }

        assertThat(success.get()).as("恰好扣满 10 次").isEqualTo(10);
        assertThat(walletService.getBalance(USER_ID)).as("终值归零，未超扣").isEqualByComparingTo("0.00");
    }
}
