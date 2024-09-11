package com.uestc.onecoupon.merchant.admin.common.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum CouponTaskSendTypeEnum {

    /**
     * 立即发送
     */
    IMMEDIATE(0),

    /**
     * 定时发送
     */
    SCHEDULED(1);

    private final int type;
}
