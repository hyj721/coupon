package com.uestc.onecoupon.merchant.admin.dto.req;

import lombok.Data;

@Data
public class CouponTemplatePageQueryReqDTO {

    /**
     * 店铺号码
     */
    private Long shopNumber;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠对象 0：商品专属 1：全店通用
     */
    private Integer target;

    /**
     * 优惠商品编码
     */
    private String goods;

    /**
     * 优惠类型 0：立减券 1：满减券 2：折扣券
     */
    private Integer type;

    /**
     * 当前页码
     */
    private Integer current;

    /**
     * 每页显示数量
     */
    private Integer size;
}