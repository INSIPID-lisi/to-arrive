package com.insipidknight.toarrive.common.exception;

import lombok.Getter;

@Getter
public enum ErrorCode {

    SUCCESS(200, "success"),
    BAD_REQUEST(400, "bad request"),
    UNAUTHORIZED(401, "unauthorized"),
    FORBIDDEN(403, "forbidden"),
    NOT_FOUND(404, "not found"),
    TOO_MANY_REQUESTS(429, "too many requests"),
    ACCOUNT_LOCKED(423, "account locked"),
    INTERNAL_ERROR(500, "internal error"),

    BUSINESS_ERROR(1000,"business error");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}
