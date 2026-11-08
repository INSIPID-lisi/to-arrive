package com.insipidknight.toarrive.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * 网关向下游转发时使用的签名密钥。
 * 下游用同一密钥校验，缺少合法签名的请求会被直接拒绝。
 */
@ConfigurationProperties(prefix = "app.security.gateway-signature")
public record GatewaySignatureProperties(String secret) {
}
