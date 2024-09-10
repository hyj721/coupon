package com.uestc.onecoupon.merchant.admin.service;

import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;

public interface ICouponTemplateService {
    void createCouponTemplate(CouponTemplateSaveReqDTO requestParam);
}
