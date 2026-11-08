package com.insipidknight.toarrive.common.auth;

/**
 * 网关转发与服务间调用的签名头。
 * 客户端传入的同名头会在网关被覆盖，下游只信任签名校验通过的值。
 */
public final class SigningHeaders {

    /** 网关签发的请求时间戳（epoch millis） */
    public static final String GATEWAY_TIMESTAMP = "X-Gateway-Timestamp";

    /** 网关对 method + path + userId + timestamp 的 HMAC 签名 */
    public static final String GATEWAY_SIGNATURE = "X-Gateway-Signature";

    /** 发起内部调用的服务名（spring.application.name） */
    public static final String INTERNAL_SERVICE = "X-Internal-Service";

    /** 内部调用时间戳（epoch millis） */
    public static final String INTERNAL_TIMESTAMP = "X-Internal-Timestamp";

    /** 调用方对 service + method + path + timestamp 的 HMAC 签名 */
    public static final String INTERNAL_SIGNATURE = "X-Internal-Signature";

    private SigningHeaders() {
    }
}
