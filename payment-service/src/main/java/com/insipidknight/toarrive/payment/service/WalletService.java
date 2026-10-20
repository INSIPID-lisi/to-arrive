package com.insipidknight.toarrive.payment.service;

import com.insipidknight.toarrive.payment.entity.Wallet;
import com.insipidknight.toarrive.payment.entity.WalletTransaction;
import com.insipidknight.toarrive.payment.entity.enums.TxType;
import com.insipidknight.toarrive.payment.mapper.WalletMapper;
import com.insipidknight.toarrive.payment.mapper.WalletTransactionMapper;
import com.insipidknight.toarrive.common.exception.BusinessException;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Slf4j
@Service
@RequiredArgsConstructor
public class WalletService {

    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final WalletMapper walletMapper;
    private final WalletTransactionMapper walletTransactionMapper;

    public BigDecimal getBalance(Long userId) {
        Wallet wallet = walletMapper.selectByUserId(userId);
        return wallet == null ? ZERO : wallet.getBalance();
    }

    @Transactional
    public BigDecimal recharge(Long userId, BigDecimal amount) {
        return credit(userId, amount, TxType.RECHARGE, null, "钱包充值");
    }

    @Transactional
    public BigDecimal credit(Long userId, BigDecimal amount, TxType type, String orderNo, String remark) {
        walletMapper.credit(userId, amount);
        BigDecimal balanceAfter = getBalance(userId);
        record(userId, type, amount, balanceAfter, orderNo, remark);
        return balanceAfter;
    }

    @Transactional
    public void deduct(Long userId, BigDecimal amount, String orderNo) {
        if (walletMapper.deduct(userId, amount) == 0) {
            throw new BusinessException("钱包余额不足");
        }
        BigDecimal balanceAfter = getBalance(userId);
        record(userId, TxType.PAY, amount, balanceAfter, orderNo, "订单支付");
    }

    private void record(Long userId, TxType type, BigDecimal amount, BigDecimal balanceAfter,
                        String orderNo, String remark) {
        WalletTransaction tx = new WalletTransaction();
        tx.setUserId(userId);
        tx.setType(type);
        tx.setAmount(amount);
        tx.setBalanceAfter(balanceAfter);
        tx.setOrderNo(orderNo);
        tx.setRemark(remark);
        walletTransactionMapper.insert(tx);
    }
}
