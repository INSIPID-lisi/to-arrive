package com.insipidknight.toarrive.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;
import java.util.Map;

/**
 * 下游服务入口隔离配置。
 *
 * <pre>
 * app:
 *   security:
 *     gateway-signature:
 *       secret: ${GATEWAY_SIGNING_SECRET}
 *       max-clock-skew: 60s
 *     internal-auth:
 *       secret: ${INTERNAL_SIGNING_SECRET}
 *       max-clock-skew: 60s
 *       allowed-calls:
 *         order-service:
 *           - /internal/user
 * </pre>
 */
@ConfigurationProperties(prefix = "app.security")
public record ServiceIsolationProperties(GatewaySignature gatewaySignature, InternalAuth internalAuth) {

    public ServiceIsolationProperties {
        gatewaySignature = gatewaySignature != null
                ? gatewaySignature
                : new GatewaySignature("", Duration.ofSeconds(60));
        internalAuth = internalAuth != null
                ? internalAuth
                : new InternalAuth("", Duration.ofSeconds(60), Map.of());
    }

    public record GatewaySignature(String secret, Duration maxClockSkew) {
        public GatewaySignature {
            if (maxClockSkew == null) {
                maxClockSkew = Duration.ofSeconds(60);
            }
        }
    }

    public record InternalAuth(String secret, Duration maxClockSkew, Map<String, List<String>> allowedCalls) {
        public InternalAuth {
            if (maxClockSkew == null) {
                maxClockSkew = Duration.ofSeconds(60);
            }
            if (allowedCalls == null) {
                allowedCalls = Map.of();
            }
        }
    }
}
