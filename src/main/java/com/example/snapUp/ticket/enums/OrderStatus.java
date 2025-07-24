package com.example.snapUp.ticket.enums;

import lombok.Getter;

/**
 * 訂單狀態
 * Order Status
 */
@Getter
public enum OrderStatus {
    PENDING("0"), // 待付款
    CONFIRMED("1"), // 已付款
    CANCELLED("2");// 取消

    private final String code;// 代號

    OrderStatus(String code) {
        this.code = code;
    }

}
