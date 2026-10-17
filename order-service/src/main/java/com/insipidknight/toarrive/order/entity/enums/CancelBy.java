package com.insipidknight.toarrive.order.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum CancelBy implements IEnum<Integer> {

    PASSENGER(1, "乘客"),
    DRIVER(2, "司机"),
    SYSTEM(3, "系统");

    private final Integer code;
    private final String desc;

    CancelBy(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static CancelBy of(Integer code) {
        if (code == null) {
            return null;
        }
        for (CancelBy cancelBy : values()) {
            if (cancelBy.code.equals(code)) {
                return cancelBy;
            }
        }
        throw new IllegalArgumentException("未知的 CancelBy code: " + code);
    }
}
