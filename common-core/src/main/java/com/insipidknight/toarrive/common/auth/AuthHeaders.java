package com.insipidknight.toarrive.common.auth;

/**
 * 网关向下游透传身份时使用的请求头。
 * 客户端同名头会在网关被清除，下游只能信任网关写入的值。
 */
public final class AuthHeaders {

    public static final String USER_ID = "X-User-Id";
    public static final String USER_UID = "X-User-Uid";
    public static final String USER_ROLE = "X-User-Role";
    public static final String TOKEN_ID = "X-Token-Id";

    private AuthHeaders() {
    }
}
