package com.insipidknight.toarrive.common.ws;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 在线会话注册表：key 为 String 形式的雪花 userId（与 JSON 出参保持一致，便于排查）。
 * MVP 单机内存实现，同一用户多设备时后者覆盖前者。
 */
@Component
public class SessionRegistry {

    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    public void register(Long userId, WebSocketSession session) {
        if (userId != null) {
            sessions.put(String.valueOf(userId), session);
        }
    }

    public void remove(Long userId, WebSocketSession session) {
        if (userId != null) {
            sessions.remove(String.valueOf(userId), session);
        }
    }

    public WebSocketSession get(Long userId) {
        return userId == null ? null : sessions.get(String.valueOf(userId));
    }
}
