package com.insipidknight.toarrive.order.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum PayStatus implements IEnum<Integer> {

    UNPAID(0, "未支付"),
    PAID(1, "已支付"),
    PAYING(2, "支付中");

    private final Integer code;
    private final String desc;

    PayStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static PayStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PayStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的 PayStatus code: " + code);
    }
}
