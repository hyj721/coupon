package com.uestc.onecoupon.merchant.admin.mapper;

import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTaskDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTaskMapper;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
public class MapperTests {
    @Resource
    CouponTaskMapper couponTaskMapper;

    @Test
    public void testSelectCouponTaskById() {
        Long id = 3L;
        CouponTaskDO couponTaskDO = couponTaskMapper.selectById(id);
        System.out.println(couponTaskDO);
    }
}
