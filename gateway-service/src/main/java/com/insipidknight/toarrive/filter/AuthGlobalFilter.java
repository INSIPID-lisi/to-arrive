package com.insipidknight.toarrive.filter;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.auth.AuthHeaders;
import com.insipidknight.toarrive.common.auth.AuthRedisKeys;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import com.insipidknight.toarrive.common.util.JwtUtil;
import com.insipidknight.toarrive.config.AuthUriProperties;
import io.jsonwebtoken.Claims;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.ReactiveStringRedisTemplate;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.PathContainer;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.util.pattern.PathPattern;
import org.springframework.web.util.pattern.PathPatternParser;
import reactor.core.publisher.Mono;
import tools.jackson.databind.ObjectMapper;

import java.util.List;

/**
 * 统一认证：验签 + Redis 会话/版本校验 + 身份头透传。
 * 下游服务只信任本过滤器写入的 X-User-* 头。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String BEARER = "Bearer ";
    private static final String WS_PATH = "/ws";

    private final AuthUriProperties authUriProperties;
    private final JwtUtil jwtUtil;
    private final ReactiveStringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private List<PathPattern> whiteList;

    @PostConstruct
    void initWhiteList() {
        PathPatternParser parser = new PathPatternParser();
        whiteList = authUriProperties.getWhiteList().stream()
                .map(parser::parse)
                .toList();
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, @NonNull GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        PathContainer path = request.getPath().pathWithinApplication();

        // 预检请求与登录/注册白名单直接放行
        if (request.getMethod() == HttpMethod.OPTIONS
                || whiteList.stream().anyMatch(pattern -> pattern.matches(path))) {
            return chain.filter(exchange);
        }

        Claims claims = jwtUtil.parseToken(resolveToken(request));
        if (claims == null || claims.get("userId") == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                    "登录状态已失效，请重新登录");
        }

        Number userId = (Number) claims.get("userId");
        String tokenId = claimAsString(claims.get("jti"));
        Number tokenVersion = claims.get("ver") instanceof Number n ? n : null;
        if (tokenId == null || tokenVersion == null) {
            return reject(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                    "登录状态已失效，请重新登录");
        }

        Mono<String> session = redisTemplate.opsForValue()
                .get(AuthRedisKeys.session(tokenId))
                .defaultIfEmpty("");
        Mono<String> version = redisTemplate.opsForValue()
                .get(AuthRedisKeys.version(userId.longValue()))
                .defaultIfEmpty("0");

        return Mono.zip(session, version)
                .flatMap(tuple -> {
                    if (tuple.getT1().isBlank() || !versionMatches(tuple.getT2(), tokenVersion)) {
                        return reject(exchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED,
                                "登录状态已失效，请重新登录");
                    }
                    ServerHttpRequest mutated = withIdentity(request, claims, userId.toString(), tokenId);
                    return chain.filter(exchange.mutate().request(mutated).build());
                })
                .onErrorResume(RedisConnectionFailureException.class, e -> {
                    log.error("鉴权 Redis 不可用", e);
                    return reject(exchange, HttpStatus.SERVICE_UNAVAILABLE, ErrorCode.INTERNAL_ERROR,
                            "认证服务暂不可用");
                });
    }

    private String resolveToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (auth != null && auth.startsWith(BEARER)) {
            return auth.substring(BEARER.length()).trim();
        }
        // WebSocket 握手无法携带自定义头，只能从 query 取
        if (WS_PATH.equals(request.getPath().value())) {
            return request.getQueryParams().getFirst("token");
        }
        return null;
    }

    private ServerHttpRequest withIdentity(ServerHttpRequest request, Claims claims,
                                           String userId, String tokenId) {
        ServerHttpRequest.Builder builder = request.mutate()
                .headers(headers -> {
                    // 先清除客户端可能伪造的同名头，再写入可信值
                    headers.remove(AuthHeaders.USER_ID);
                    headers.remove(AuthHeaders.USER_UID);
                    headers.remove(AuthHeaders.USER_ROLE);
                    headers.remove(AuthHeaders.TOKEN_ID);
                    headers.remove(HttpHeaders.AUTHORIZATION);
                })
                .header(AuthHeaders.USER_ID, userId)
                .header(AuthHeaders.TOKEN_ID, tokenId);

        String uid = claimAsString(claims.get("uid"));
        if (uid != null) {
            builder.header(AuthHeaders.USER_UID, uid);
        }
        String role = claimAsString(claims.get("role"));
        if (role != null) {
            builder.header(AuthHeaders.USER_ROLE, role);
        }
        return builder.build();
    }

    private boolean versionMatches(String current, Number tokenVersion) {
        try {
            return Long.parseLong(current.trim()) == tokenVersion.longValue();
        } catch (NumberFormatException e) {
            log.warn("Redis 中的 token 版本号非法: {}", current);
            return false;
        }
    }

    private String claimAsString(Object value) {
        return value == null ? null : value.toString();
    }

    private Mono<Void> reject(ServerWebExchange exchange, HttpStatus status,
                              ErrorCode errorCode, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = objectMapper.writeValueAsBytes(Result.error(errorCode, message));
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body)));
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
