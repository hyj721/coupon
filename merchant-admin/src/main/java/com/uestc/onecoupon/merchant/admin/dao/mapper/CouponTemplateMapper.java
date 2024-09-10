package com.uestc.onecoupon.merchant.admin.dao.mapper;

import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponTemplateMapper {
    int insert(CouponTemplateDO couponTemplateDO);
}
