package com.insipidknight.toarrive.wallet.service;

import com.insipidknight.toarrive.common.exception.BusinessException;
import com.insipidknight.toarrive.wallet.entity.Wallet;
import com.insipidknight.toarrive.wallet.entity.WalletTransaction;
import com.insipidknight.toarrive.wallet.entity.enums.TxType;
import com.insipidknight.toarrive.wallet.mapper.WalletMapper;
import com.insipidknight.toarrive.wallet.mapper.WalletTransactionMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WalletServiceTest {

    private static final Long USER_ID = 100L;

    @Mock
    private WalletMapper walletMapper;

    @Mock
    private WalletTransactionMapper walletTransactionMapper;

    @InjectMocks
    private WalletService walletService;

    @Test
    @DisplayName("查余额：无钱包行 → 返回 0.00")
    void getBalance_noWallet() {
        when(walletMapper.selectByUserId(USER_ID)).thenReturn(null);

        assertThat(walletService.getBalance(USER_ID)).isEqualByComparingTo("0.00");
    }

    @Test
    @DisplayName("充值：入账并落一笔 RECHARGE 流水")
    void recharge_success() {
        when(walletMapper.selectByUserId(USER_ID)).thenReturn(wallet("50.00"));

        BigDecimal balance = walletService.recharge(USER_ID, new BigDecimal("50.00"));

        assertThat(balance).isEqualByComparingTo("50.00");
        verify(walletMapper).credit(USER_ID, new BigDecimal("50.00"));

        WalletTransaction tx = captureTransaction();
        assertThat(tx.getType()).isEqualTo(TxType.RECHARGE);
        assertThat(tx.getAmount()).isEqualByComparingTo("50.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("50.00");
        assertThat(tx.getOrderNo()).isNull();
    }

    @Test
    @DisplayName("扣款：余额不足（原子 UPDATE 0 行）→ 报错且不写流水")
    void deduct_insufficientBalance() {
        when(walletMapper.deduct(USER_ID, new BigDecimal("10.00"))).thenReturn(0);

        assertThatThrownBy(() -> walletService.deduct(USER_ID, new BigDecimal("10.00"), "TA123"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("钱包余额不足");

        verifyNoInteractions(walletTransactionMapper);
    }

    @Test
    @DisplayName("扣款成功：落一笔 PAY 流水并关联订单号")
    void deduct_success() {
        when(walletMapper.deduct(USER_ID, new BigDecimal("10.00"))).thenReturn(1);
        when(walletMapper.selectByUserId(USER_ID)).thenReturn(wallet("90.00"));

        walletService.deduct(USER_ID, new BigDecimal("10.00"), "TA123");

        WalletTransaction tx = captureTransaction();
        assertThat(tx.getType()).isEqualTo(TxType.PAY);
        assertThat(tx.getAmount()).isEqualByComparingTo("10.00");
        assertThat(tx.getBalanceAfter()).isEqualByComparingTo("90.00");
        assertThat(tx.getOrderNo()).isEqualTo("TA123");
    }

    // ---------- helpers ----------

    private WalletTransaction captureTransaction() {
        ArgumentCaptor<WalletTransaction> captor = ArgumentCaptor.forClass(WalletTransaction.class);
        verify(walletTransactionMapper).insert(captor.capture());
        return captor.getValue();
    }

    private static Wallet wallet(String balance) {
        Wallet wallet = new Wallet();
        wallet.setUserId(USER_ID);
        wallet.setBalance(new BigDecimal(balance));
        return wallet;
    }
}
