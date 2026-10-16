package com.insipidknight.toarrive.payment.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class WalletVO {

    private Long userId;
    private BigDecimal balance;

    public WalletVO(Long userId, BigDecimal balance) {
        this.userId = userId;
        this.balance = balance;
    }
}
