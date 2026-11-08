package com.insipidknight.toarrive.common.filter;

import com.insipidknight.toarrive.common.Result;
import com.insipidknight.toarrive.common.auth.AuthHeaders;
import com.insipidknight.toarrive.common.auth.RequestSignatures;
import com.insipidknight.toarrive.common.auth.SigningHeaders;
import com.insipidknight.toarrive.common.config.ServiceIsolationProperties;
import com.insipidknight.toarrive.common.exception.ErrorCode;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.Ordered;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;
import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.time.Duration;
import java.util.List;

/**
 * 下游服务入口隔离：
 * <ul>
 *   <li>{@code /api/**}、{@code /ws} 必须携带网关签名，拒绝绕过网关的直接访问；</li>
 *   <li>{@code /internal/**} 必须携带调用方服务签名，并按 allowlist 限制可调用的下游路径。</li>
 * </ul>
 */
@Slf4j
@RequiredArgsConstructor
public class ServiceIsolationFilter extends OncePerRequestFilter implements Ordered {

    private static final String GATEWAY_DOMAIN = "gateway-v1";
    private static final String INTERNAL_DOMAIN = "internal-v1";

    private final ServiceIsolationProperties properties;
    private final ObjectMapper objectMapper;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();

        if (path.startsWith("/internal/")) {
            if (!verifyInternal(request, response, path)) {
                return;
            }
        } else if (isExternalPath(path)) {
            if (!verifyGateway(request, response, path)) {
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean isExternalPath(String path) {
        return path.startsWith("/api/") || path.equals("/ws") || path.startsWith("/ws/");
    }

    private boolean verifyGateway(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        String timestamp = request.getHeader(SigningHeaders.GATEWAY_TIMESTAMP);
        String signature = request.getHeader(SigningHeaders.GATEWAY_SIGNATURE);
        String userId = request.getHeader(AuthHeaders.USER_ID);

        if (!isFresh(timestamp, properties.gatewaySignature().maxClockSkew())
                || !RequestSignatures.verify(properties.gatewaySignature().secret(), signature,
                GATEWAY_DOMAIN, timestamp, request.getMethod(), path, userId == null ? "" : userId)) {
            log.warn("网关签名校验失败: {} {}", request.getMethod(), path);
            return reject(response, 401, ErrorCode.UNAUTHORIZED, "请求必须经由网关访问");
        }
        return true;
    }

    private boolean verifyInternal(HttpServletRequest request, HttpServletResponse response, String path)
            throws IOException {
        String caller = request.getHeader(SigningHeaders.INTERNAL_SERVICE);
        String timestamp = request.getHeader(SigningHeaders.INTERNAL_TIMESTAMP);
        String signature = request.getHeader(SigningHeaders.INTERNAL_SIGNATURE);

        if (!StringUtils.hasText(caller) || !isFresh(timestamp, properties.internalAuth().maxClockSkew())
                || !RequestSignatures.verify(properties.internalAuth().secret(), signature,
                INTERNAL_DOMAIN, caller, timestamp, request.getMethod(), path)) {
            log.warn("内部调用凭证无效: caller={} {} {}", caller, request.getMethod(), path);
            return reject(response, 401, ErrorCode.UNAUTHORIZED, "内部调用凭证无效");
        }

        if (!isCallerAllowed(caller, path)) {
            log.warn("内部调用未被授权: caller={} path={}", caller, path);
            return reject(response, 403, ErrorCode.FORBIDDEN, "内部调用未被授权");
        }
        return true;
    }

    private boolean isCallerAllowed(String caller, String path) {
        List<String> allowed = properties.internalAuth().allowedCalls().get(caller);
        if (allowed == null) {
            return false;
        }
        return allowed.stream().anyMatch(prefix ->
                path.equals(prefix) || path.startsWith(prefix + "/"));
    }

    private boolean isFresh(String timestamp, Duration maxClockSkew) {
        if (!StringUtils.hasText(timestamp)) {
            return false;
        }
        try {
            long sentAt = Long.parseLong(timestamp);
            return Math.abs(System.currentTimeMillis() - sentAt) <= maxClockSkew.toMillis();
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private boolean reject(HttpServletResponse response, int httpStatus, ErrorCode errorCode, String message)
            throws IOException {
        response.setStatus(httpStatus);
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(objectMapper.writeValueAsString(Result.error(errorCode, message)));
        return false;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }
}
