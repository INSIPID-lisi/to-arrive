package com.insipidknight.toarrive.payment.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum PaymentOrderStatus implements IEnum<Integer> {

    PROCESSING(1, "处理中"),
    SUCCESS(2, "成功"),
    FAILED(3, "失败");

    private final Integer code;
    private final String desc;

    PaymentOrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static PaymentOrderStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (PaymentOrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的 PaymentOrderStatus code: " + code);
    }
}
