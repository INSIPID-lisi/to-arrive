package com.insipidknight.toarrive.order.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum OrderStatus implements IEnum<Integer> {

    PENDING(1, "待接单"),
    ACCEPTED(2, "已接单"),
    ARRIVED(3, "已到达"),
    IN_TRIP(4, "行程中"),
    FINISHED(5, "已完成"),
    CANCELLED(6, "已取消");

    private final Integer code;
    private final String desc;

    OrderStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static OrderStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (OrderStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的 OrderStatus code: " + code);
    }
}
