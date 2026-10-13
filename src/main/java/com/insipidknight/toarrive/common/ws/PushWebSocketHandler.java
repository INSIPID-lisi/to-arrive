package com.insipidknight.toarrive.common.ws;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

/**
 * 会话生命周期管理：连接建立时登记、断开时摘除。业务推送统一走 {@link PushService}。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class PushWebSocketHandler extends TextWebSocketHandler {

    private final SessionRegistry sessionRegistry;

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long userId = userIdOf(session);
        sessionRegistry.register(userId, session);
        log.debug("WebSocket 已连接 userId={}", userId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long userId = userIdOf(session);
        sessionRegistry.remove(userId, session);
        log.debug("WebSocket 已断开 userId={}", userId);
    }

    private Long userIdOf(WebSocketSession session) {
        return (Long) session.getAttributes().get(JwtHandshakeInterceptor.ATTR_USER_ID);
    }
}
