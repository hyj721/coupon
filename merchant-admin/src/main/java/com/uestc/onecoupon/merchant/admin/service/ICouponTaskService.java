package com.uestc.onecoupon.merchant.admin.service;

import com.uestc.onecoupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;

public interface ICouponTaskService {

    /**
     * 商家创建优惠券推送任务
     *
     * @param requestParam 请求参数
     */
    void createCouponTask(CouponTaskCreateReqDTO requestParam);
}
