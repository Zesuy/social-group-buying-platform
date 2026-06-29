package com.example.groupshop.common.enums;

import com.example.groupshop.common.exception.BusinessException;
import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * Error code definitions.
 * Maps each error code to an HTTP status and a default message.
 */
@Getter
@AllArgsConstructor
public enum ErrorCode {

    // ── General errors ──────────────────────────────────────────────
    VALIDATION_ERROR(400, "VALIDATION_ERROR", "参数校验失败"),
    UNAUTHORIZED(401, "UNAUTHORIZED", "未登录或令牌无效"),
    FORBIDDEN(403, "FORBIDDEN", "无权限操作"),
    RESOURCE_NOT_FOUND(404, "RESOURCE_NOT_FOUND", "资源不存在"),
    RESOURCE_CONFLICT(409, "RESOURCE_CONFLICT", "状态冲突或重复创建"),
    BUSINESS_RULE_VIOLATION(422, "BUSINESS_RULE_VIOLATION", "不满足业务规则"),
    INTERNAL_ERROR(500, "INTERNAL_ERROR", "服务端异常"),

    // ── Business errors ────────────────────────────────────────────
    STORE_ALREADY_EXISTS(409, "STORE_ALREADY_EXISTS", "当前用户已创建店铺"),
    LEADER_REQUIRED(403, "LEADER_REQUIRED", "当前操作需要团长身份"),
    STORE_FORBIDDEN(403, "STORE_FORBIDDEN", "不能操作他人店铺"),

    GROUP_BUY_NOT_PURCHASABLE(422, "GROUP_BUY_NOT_PURCHASABLE", "团购不可购买"),
    GROUP_BUY_ENDED(422, "GROUP_BUY_ENDED", "团购已结束"),
    ITEM_NOT_IN_GROUP_BUY(422, "ITEM_NOT_IN_GROUP_BUY", "下单商品不属于该团购"),

    ADDRESS_FORBIDDEN(403, "ADDRESS_FORBIDDEN", "收货地址不属于当前用户"),

    ORDER_NOT_PAYABLE(422, "ORDER_NOT_PAYABLE", "订单不可支付"),
    ORDER_ALREADY_PAID(409, "ORDER_ALREADY_PAID", "订单已支付，重复支付请求不应再次扣库存"),
    ORDER_NOT_CANCELABLE(422, "ORDER_NOT_CANCELABLE", "订单不可取消"),
    ORDER_NOT_SHIPPABLE(422, "ORDER_NOT_SHIPPABLE", "订单不可发货"),
    ORDER_ALREADY_SHIPPED(409, "ORDER_ALREADY_SHIPPED", "订单已发货，重复发货请求不应再次创建发货记录"),

    ORDER_NOT_COMPLETABLE(422, "ORDER_NOT_COMPLETABLE", "订单不可确认收货"),
    ORDER_ALREADY_COMPLETED(409, "ORDER_ALREADY_COMPLETED", "订单已完成，重复确认收货请求不应再次确认"),

    INSUFFICIENT_STOCK(422, "INSUFFICIENT_STOCK", "团购商品库存不足"),
    SUBSCRIPTION_EXISTS(409, "SUBSCRIPTION_EXISTS", "已订阅该团长"),
    ;

    private final int httpStatus;
    private final String code;
    private final String defaultMessage;

    /**
     * Build a BusinessException with the default message.
     */
    public BusinessException toException() {
        return new BusinessException(this);
    }

    /**
     * Build a BusinessException with a custom message.
     */
    public BusinessException toException(String message) {
        return new BusinessException(this, message);
    }
}
