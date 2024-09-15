package com.uestc.onecoupon.engine.dto.req;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTemplateQueryReqDTO {

    /**
     * 店铺编号
     */
    private String shopNumber;

    /**
     * 优惠券模板id
     */
    private String couponTemplateId;
}