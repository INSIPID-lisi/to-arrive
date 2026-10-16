package com.insipidknight.toarrive.common.enums;

import com.baomidou.mybatisplus.annotation.EnumValue;
import lombok.Getter;

@Getter
public enum UserRole {

    PASSENGER(1, "乘客"),
    DRIVER(2, "司机"),
    ADMIN(3, "管理员");

    @EnumValue
    private final Integer code;
    private final String desc;

    UserRole(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }


    public Integer getValue() {
        return this.code;
    }

    public static UserRole of(Integer code) {
        if (code == null) {
            return null;
        }
        for (UserRole role : values()) {
            if (role.code.equals(code)) {
                return role;
            }
        }
        throw new IllegalArgumentException("未知的 UserRole code: " + code);
    }
}