package com.insipidknight.toarrive.filter;

import com.insipidknight.toarrive.common.auth.AuthHeaders;
import com.insipidknight.toarrive.common.auth.RequestSignatures;
import com.insipidknight.toarrive.common.auth.SigningHeaders;
import com.insipidknight.toarrive.config.GatewaySignatureProperties;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * 在鉴权过滤器写入可信身份头之后，对转发请求签名。
 * 下游用同一密钥校验 method + path + userId + timestamp，拒绝绕过网关的直连。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class GatewaySignatureFilter implements GlobalFilter, Ordered {

    private static final String GATEWAY_DOMAIN = "gateway-v1";

    private final GatewaySignatureProperties properties;

    @PostConstruct
    void checkSecret() {
        if (properties.secret() == null || properties.secret().isBlank()) {
            throw new IllegalStateException(
                    "app.security.gateway-signature.secret 未配置，拒绝在无签名保护下启动网关");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        String userId = request.getHeaders().getFirst(AuthHeaders.USER_ID);
        long timestamp = System.currentTimeMillis();
        String signature = RequestSignatures.sign(properties.secret(), GATEWAY_DOMAIN,
                Long.toString(timestamp), request.getMethod().name(), path, userId == null ? "" : userId);

        ServerHttpRequest mutated = request.mutate()
                .headers(headers -> {
                    // 覆盖客户端可能伪造的签名头
                    headers.set(SigningHeaders.GATEWAY_TIMESTAMP, Long.toString(timestamp));
                    headers.set(SigningHeaders.GATEWAY_SIGNATURE, signature);
                })
                .build();

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    @Override
    public int getOrder() {
        // 必须晚于 AuthGlobalFilter(-100)，签名里要带上它写入的可信 userId
        return -90;
    }
}
