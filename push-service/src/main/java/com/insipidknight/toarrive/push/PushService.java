package com.insipidknight.toarrive.push;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import tools.jackson.databind.json.JsonMapper;

@Slf4j
@Service
@RequiredArgsConstructor
public class PushService {

    private final SessionRegistry sessionRegistry;
    private final JsonMapper jsonMapper;

    /**
     * 向指定用户推送消息；对方不在线（无会话/会话已关）时静默跳过，不影响业务主流程。
     */
    public void sendToUser(Long userId, WsMessage message) {
        WebSocketSession session = sessionRegistry.get(userId);
        if (session == null || !session.isOpen()) {
            return;
        }
        try {
            String payload = jsonMapper.writeValueAsString(message);
            // WebSocketSession 并发发送非线程安全，单会话内串行化
            synchronized (session) {
                session.sendMessage(new TextMessage(payload));
            }
        } catch (Exception e) {
            log.warn("WebSocket 推送失败 userId={}, type={}: {}", userId, message.type(), e.getMessage());
        }
    }
}
