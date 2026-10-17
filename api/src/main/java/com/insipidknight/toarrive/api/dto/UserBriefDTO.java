package com.insipidknight.toarrive.api.dto;

import com.insipidknight.toarrive.common.enums.UserRole;

/**
 * 跨服务的用户简要信息，供订单等场景组装对方展示信息使用。
 */
public record UserBriefDTO(String uid, String displayName, String avatarUrl, UserRole role, String maskedPhone) {
}
