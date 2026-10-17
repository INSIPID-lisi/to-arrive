package com.insipidknight.toarrive.user.service;

import com.insipidknight.toarrive.common.auth.AuthRedisKeys;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

/**
 * Token 会话与版本号存储。
 * - auth:sess:{jti} 存在才允许访问（登出/过期即删除）
 * - auth:ver:{userId} 递增后，该用户所有旧 token 立即失效
 */
@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final StringRedisTemplate redisTemplate;

    public long currentVersion(Long userId) {
        String value = redisTemplate.opsForValue().get(AuthRedisKeys.version(userId));
        return value == null ? 0 : Long.parseLong(value);
    }

    public void saveSession(Long userId, String tokenId, long ttlMillis) {
        redisTemplate.opsForValue().set(
                AuthRedisKeys.session(tokenId),
                String.valueOf(userId),
                Duration.ofMillis(ttlMillis));
    }

    public void removeSession(String tokenId) {
        if (tokenId != null && !tokenId.isBlank()) {
            redisTemplate.delete(AuthRedisKeys.session(tokenId));
        }
    }

    public void revokeAll(Long userId) {
        redisTemplate.opsForValue().increment(AuthRedisKeys.version(userId));
    }
}
