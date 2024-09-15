package com.uestc.onecoupon.engine.dao.mapper;

import com.uestc.onecoupon.engine.dao.entity.CouponTemplateDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponTemplateMapper{
    CouponTemplateDO query(CouponTemplateDO couponTemplateDOReq);
}