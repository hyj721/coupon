package com.uestc.onecoupon.engine.service;

import com.uestc.onecoupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.uestc.onecoupon.engine.dto.resp.CouponTemplateQueryRespDTO;

public interface ICouponTemplateService {
    CouponTemplateQueryRespDTO findCouponTemplate(CouponTemplateQueryReqDTO requestParam);
}
