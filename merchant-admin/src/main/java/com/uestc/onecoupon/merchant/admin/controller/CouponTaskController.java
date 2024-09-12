package com.uestc.onecoupon.merchant.admin.controller;

import com.uestc.coupon.framework.idempotent.NoDuplicateSubmit;
import com.uestc.coupon.framework.result.Result;
import com.uestc.coupon.framework.web.Results;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.uestc.onecoupon.merchant.admin.service.ICouponTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api")
public class CouponTaskController {
    private final ICouponTaskService couponTaskService;

    @PostMapping("/merchant-admin/coupon-task/create")
    @NoDuplicateSubmit(message = "请勿短时间内重复提交优惠券推送任务")
    public Result<Void> createCouponTask(@RequestBody CouponTaskCreateReqDTO requestParam) {
        long startTime = System.nanoTime();
        couponTaskService.createCouponTask(requestParam);
        long totalTime = System.nanoTime();
        System.out.printf("总时间: %.2f ms%n", (totalTime - startTime) / 1e6);
        return Results.success();

    }
}
