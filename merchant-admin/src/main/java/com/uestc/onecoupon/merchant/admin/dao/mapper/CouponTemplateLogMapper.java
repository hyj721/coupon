package com.uestc.onecoupon.merchant.admin.dao.mapper;

import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTemplateLogDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponTemplateLogMapper {
    int insert(CouponTemplateLogDO couponTemplateLogDO);
}