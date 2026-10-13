package com.insipidknight.toarrive.user.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum WorkStatus implements IEnum<Integer> {

    OFFLINE(0, "收车"),
    ONLINE(1, "出车");

    private final Integer code;
    private final String desc;

    WorkStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static WorkStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (WorkStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的 WorkStatus code: " + code);
    }
}
