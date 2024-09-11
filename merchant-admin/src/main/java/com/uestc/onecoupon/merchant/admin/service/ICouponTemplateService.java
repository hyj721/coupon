package com.uestc.onecoupon.merchant.admin.service;

import com.github.pagehelper.PageInfo;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;

public interface ICouponTemplateService {
    void createCouponTemplate(CouponTemplateSaveReqDTO requestParam);

    PageInfo<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam);

    CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId);

    void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam);

    void terminateCouponTemplate(String couponTemplateId);
}
