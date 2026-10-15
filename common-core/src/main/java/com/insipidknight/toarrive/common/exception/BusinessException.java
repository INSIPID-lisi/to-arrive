package com.insipidknight.toarrive.common.exception;

import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {

    private final int code;

    public BusinessException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public BusinessException(ErrorCode errorCode, String customMessage) {
        super(customMessage);
        this.code = errorCode.getCode();
    }

    public BusinessException(String customMessage) {
        super(customMessage);
        this.code = ErrorCode.BUSINESS_ERROR.getCode();
    }
}
