package com.insipidknight.toarrive.user.entity.enums;


import com.baomidou.mybatisplus.annotation.IEnum;
import lombok.Getter;

@Getter
public enum VerifyStatus implements IEnum<Integer> {

    PENDING(1, "待审核"),
    APPROVED(2, "已通过"),
    REJECTED(3, "已驳回");

    private final Integer code;
    private final String desc;

    VerifyStatus(Integer code, String desc) {
        this.code = code;
        this.desc = desc;
    }

    @Override
    public Integer getValue() {
        return this.code;
    }

    public static VerifyStatus of(Integer code) {
        if (code == null) {
            return null;
        }
        for (VerifyStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("未知的 VerifyStatus code: " + code);
    }
}
