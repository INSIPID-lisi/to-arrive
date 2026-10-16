package com.insipidknight.toarrive.push;

import com.insipidknight.toarrive.common.util.JwtUtil;
import io.jsonwebtoken.Claims;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * 握手鉴权：WebSocket 无法携带自定义头，故从 query 参数 ?token= 解析 JWT。
 */
@Component
public class JwtHandshakeInterceptor implements HandshakeInterceptor {

    public static final String ATTR_USER_ID = "userId";

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        Claims claims = JwtUtil.parseToken(extractToken(request.getURI().getQuery()));
        if (claims == null || claims.get("userId") == null) {
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return false;
        }
        attributes.put(ATTR_USER_ID, ((Number) claims.get("userId")).longValue());
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }

    private String extractToken(String query) {
        if (query == null || query.isEmpty()) {
            return null;
        }
        for (String pair : query.split("&")) {
            int idx = pair.indexOf('=');
            if (idx > 0 && "token".equals(pair.substring(0, idx))) {
                return URLDecoder.decode(pair.substring(idx + 1), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
