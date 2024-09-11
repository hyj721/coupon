package com.uestc.onecoupon.merchant.admin.dao.mapper;

import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CouponTemplateMapper {
    int insert(CouponTemplateDO couponTemplateDO);

    List<CouponTemplatePageQueryRespDTO> queryPage(CouponTemplatePageQueryReqDTO requestParam);

    CouponTemplateDO query(CouponTemplateDO couponTemplateDOReq);

    int increaseNumberCouponTemplate(@Param("couponTemplateDOReq") CouponTemplateDO couponTemplateDOReq, @Param("number") Integer number);

    void updateCouponTemplateEnd(CouponTemplateDO couponTemplateDOReq);
}