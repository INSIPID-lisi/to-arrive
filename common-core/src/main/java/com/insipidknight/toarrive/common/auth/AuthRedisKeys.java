package com.insipidknight.toarrive.common.auth;

/**
 * 鉴权会话在 Redis 中的键约定，网关与 user-service 共用。
 */
public final class AuthRedisKeys {

    public static final String SESSION_PREFIX = "auth:sess:";
    public static final String VERSION_PREFIX = "auth:ver:";

    private AuthRedisKeys() {
    }

    /** 单次登录会话，value = userId，TTL 与 token 剩余有效期一致 */
    public static String session(String tokenId) {
        return SESSION_PREFIX + tokenId;
    }

    /** 用户 token 版本号，撤销全部会话时递增 */
    public static String version(Long userId) {
        return VERSION_PREFIX + userId;
    }
}
