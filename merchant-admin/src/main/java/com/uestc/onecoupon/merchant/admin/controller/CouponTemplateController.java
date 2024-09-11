package com.uestc.onecoupon.merchant.admin.controller;

import com.github.pagehelper.PageInfo;
import com.uestc.coupon.framework.idempotent.NoDuplicateSubmit;
import com.uestc.coupon.framework.result.Result;
import com.uestc.coupon.framework.web.Results;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.service.ICouponTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CouponTemplateController {

    private final ICouponTemplateService couponTemplateService;


    @NoDuplicateSubmit(message = "您操作太快，请稍后再试")
    @PostMapping("/api/merchant-admin/coupon-template/create")
    public Result<Void> createCouponTemplate(@RequestBody CouponTemplateSaveReqDTO requestParam) {
        couponTemplateService.createCouponTemplate(requestParam);
        return Results.success();
    }

    @GetMapping("/api/merchant-admin/coupon-template/page")
    public Result<PageInfo<CouponTemplatePageQueryRespDTO>> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        return Results.success(couponTemplateService.pageQueryCouponTemplate(requestParam));
    }

    @GetMapping("/api/merchant-admin/coupon-template/find")
    public Result<CouponTemplateQueryRespDTO> findCouponTemplate(String couponTemplateId) {
        return Results.success(couponTemplateService.findCouponTemplateById(couponTemplateId));
    }

    @NoDuplicateSubmit(message = "请勿短时间内重复增加优惠券发行量")
    @PostMapping("/api/merchant-admin/coupon-template/increase-number")
    public Result<Void> increaseNumberCouponTemplate(@RequestBody CouponTemplateNumberReqDTO requestParam) {
        couponTemplateService.increaseNumberCouponTemplate(requestParam);
        return Results.success();
    }

    @PostMapping("/api/merchant-admin/coupon-template/terminate")
    public Result<Void> terminateCouponTemplate(String couponTemplateId) {
        couponTemplateService.terminateCouponTemplate(couponTemplateId);
        return Results.success();
    }


}
