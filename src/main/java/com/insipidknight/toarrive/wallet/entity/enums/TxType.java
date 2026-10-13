package com.insipidknight.toarrive.wallet.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum TxType implements IEnum<Integer> {

    RECHARGE(1, "充值"),
    PAY(2, "支付");

    private final Integer code;
    private final String desc;

    TxType(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static TxType of(Integer code) {
        if (code == null) {
            return null;
        }
        for (TxType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("未知的 TxType code: " + code);
    }
}
