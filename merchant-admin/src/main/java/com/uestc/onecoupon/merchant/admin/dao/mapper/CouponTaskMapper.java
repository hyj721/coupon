package com.uestc.onecoupon.merchant.admin.dao.mapper;

import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTaskDO;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface CouponTaskMapper {
    void insert(CouponTaskDO couponTaskDO);

    void updateById(CouponTaskDO updateCouponTaskDO);

    CouponTaskDO selectById(Long id);

    List<CouponTaskDO> selectPendingTasks(long initId, int maxLimit);
}
