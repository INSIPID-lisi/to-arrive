package com.insipidknight.toarrive.push;

/**
 * WebSocket 推送的通用外壳：仅承载「类型 + 订单号 + 状态 + 载荷」，
 * 不引用任何业务实体，保证 common 不反向依赖业务模块。
 */
public record WsMessage(String type, String orderNo, String status, Object data) {
}
