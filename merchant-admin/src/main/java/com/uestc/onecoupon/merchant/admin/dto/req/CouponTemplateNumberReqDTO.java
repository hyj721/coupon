package com.uestc.onecoupon.merchant.admin.dto.req;

import lombok.Data;

@Data
public class CouponTemplateNumberReqDTO {

    /**
     * 优惠券模板id
     */
    private String couponTemplateId;

    /**
     * 增加发行数量
     */
    private Integer number;
}