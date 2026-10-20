package com.insipidknight.toarrive.payment.service;

import com.insipidknight.toarrive.api.dto.PaymentRequest;
import com.insipidknight.toarrive.api.dto.PaymentStatusDTO;
import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.payment.entity.PaymentOrder;
import com.insipidknight.toarrive.payment.entity.enums.PaymentOrderStatus;
import com.insipidknight.toarrive.payment.entity.enums.TxType;
import com.insipidknight.toarrive.payment.mapper.PaymentOrderMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * 订单结算：扣乘客、给司机入账、记平台抽成。
 * 三笔资金变动都在 payment_db 内，用同一个本地事务保证原子性；
 * payment_order.order_no 唯一键作为跨服务重试的幂等锚点。
 */
@Slf4j
@Service
public class SettlementService {

    /**
     * 平台账户使用保留 ID 0，不会与雪花用户 ID 冲突；该账户不出现在任何面向用户的接口中。
     */
    private static final Long PLATFORM_ACCOUNT_ID = 0L;
    private static final int MONEY_SCALE = 2;

    private final PaymentOrderMapper paymentOrderMapper;
    private final WalletService walletService;
    private final TransactionTemplate transactionTemplate;
    private final BigDecimal commissionRate;

    public SettlementService(PaymentOrderMapper paymentOrderMapper,
                             WalletService walletService,
                             PlatformTransactionManager transactionManager,
                             @Value("${app.commission.rate:0.20}") BigDecimal commissionRate) {
        this.paymentOrderMapper = paymentOrderMapper;
        this.walletService = walletService;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.commissionRate = commissionRate;
    }

    public void settle(PaymentRequest request) {
        validate(request);
        if (isAlreadySuccess(request.orderNo())) {
            return;
        }

        try {
            transactionTemplate.executeWithoutResult(status -> doSettle(request));
        } catch (DuplicateKeyException e) {
            // 并发重试：另一笔请求已提交，读到成功结果就按幂等返回
            if (isAlreadySuccess(request.orderNo())) {
                return;
            }
            throw new BusinessException("支付正在处理中，请稍后查询");
        }
    }

    public PaymentStatusDTO getStatus(String orderNo) {
        PaymentOrder order = paymentOrderMapper.selectById(orderNo);
        if (order == null) {
            return null;
        }
        return new PaymentStatusDTO(order.getOrderNo(), order.getStatus().getValue(),
                order.getAmount(), order.getCommission(), order.getDriverIncome());
    }

    private void doSettle(PaymentRequest request) {
        BigDecimal amount = request.amount().setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal commission = amount.multiply(commissionRate).setScale(MONEY_SCALE, RoundingMode.HALF_UP);
        BigDecimal driverIncome = amount.subtract(commission);

        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(request.orderNo());
        order.setPassengerId(request.payerId());
        order.setDriverId(request.driverId());
        order.setAmount(amount);
        order.setCommissionRate(commissionRate);
        order.setCommission(commission);
        order.setDriverIncome(driverIncome);
        order.setStatus(PaymentOrderStatus.PROCESSING);
        paymentOrderMapper.insert(order);

        walletService.deduct(request.payerId(), amount, request.orderNo());
        if (driverIncome.signum() > 0) {
            walletService.credit(request.driverId(), driverIncome,
                    TxType.DRIVER_INCOME, request.orderNo(), "订单收入");
        }
        if (commission.signum() > 0) {
            walletService.credit(PLATFORM_ACCOUNT_ID, commission,
                    TxType.COMMISSION, request.orderNo(), "平台抽成");
        }

        order.setStatus(PaymentOrderStatus.SUCCESS);
        paymentOrderMapper.updateById(order);
        log.info("订单结算完成 orderNo={}, amount={}, commission={}, driverIncome={}",
                request.orderNo(), amount, commission, driverIncome);
    }

    private boolean isAlreadySuccess(String orderNo) {
        PaymentOrder existing = paymentOrderMapper.selectById(orderNo);
        return existing != null && existing.getStatus() == PaymentOrderStatus.SUCCESS;
    }

    private void validate(PaymentRequest request) {
        if (request == null || request.orderNo() == null || request.payerId() == null
                || request.driverId() == null || request.amount() == null
                || request.amount().signum() <= 0) {
            throw new BusinessException("非法的支付请求");
        }
    }
}
