package com.insipidknight.toarrive.user.entity.enums;

import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum UserStatus implements IEnum<Integer> {

    NORMAL(1, "正常"),
    DISABLED(0, "禁用"),
    CANCELLED(2, "注销");

    private final Integer code;
    private final String desc;

    UserStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static UserStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的 UserStatus code: " + code);
    }
}