package com.insipidknight.toarrive.api.dto;

/**
 * WebSocket 推送的通用外壳：仅承载「类型 + 订单号 + 状态 + 载荷」，
 * 不引用任何业务实体，保证契约不与业务模块耦合。
 */
public record WsMessage(String type, String orderNo, String status, Object data) {
}
