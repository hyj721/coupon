package com.uestc.onecoupon.engine.controller;

import com.uestc.onecoupon.engine.dto.req.CouponTemplateQueryReqDTO;
import com.uestc.onecoupon.engine.dto.resp.CouponTemplateQueryRespDTO;
import com.uestc.onecoupon.engine.service.ICouponTemplateService;
import com.uestc.coupon.framework.result.Result;
import com.uestc.coupon.framework.web.Results;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponTemplateController {
    private final ICouponTemplateService couponTemplateService;

    @GetMapping("/api/engine/coupon-template/query")
    public Result<CouponTemplateQueryRespDTO> findCouponTemplate(CouponTemplateQueryReqDTO requestParam) {
        return Results.success(couponTemplateService.findCouponTemplate(requestParam));
    }
}
