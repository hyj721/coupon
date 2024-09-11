package com.uestc.onecoupon.merchant.admin.dao.mapper;

import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTaskDO;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface CouponTaskMapper {
    void insert(CouponTaskDO couponTaskDO);
}
