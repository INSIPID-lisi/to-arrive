package com.insipidknight.toarrive.common.context;

import com.insipidknight.toarrive.common.enums.UserRole;

public class UserContext {
    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();
    private static final ThreadLocal<UserRole> ROLE = new ThreadLocal<>();
    private static final ThreadLocal<String> TOKEN_ID = new ThreadLocal<>();

    private UserContext() {
    }

    public static void setUserId(Long userId) {
        USER_ID.set(userId);
    }

    public static Long getUserId() {
        return USER_ID.get();
    }

    public static void setRole(UserRole role) {
        ROLE.set(role);
    }

    public static UserRole getRole() {
        return ROLE.get();
    }

    public static void setTokenId(String tokenId) {
        TOKEN_ID.set(tokenId);
    }

    public static String getTokenId() {
        return TOKEN_ID.get();
    }

    public static void clear() {
        USER_ID.remove();
        ROLE.remove();
        TOKEN_ID.remove();
    }
}
