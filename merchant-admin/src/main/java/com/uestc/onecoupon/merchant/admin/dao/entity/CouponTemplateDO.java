package com.uestc.onecoupon.merchant.admin.dao.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CouponTemplateDO {

    /**
     * id
     */
    private Long id;

    /**
     * 店铺编号
     */
    private Long shopNumber;

    /**
     * 优惠券名称
     */
    private String name;

    /**
     * 优惠券来源 0：店铺券 1：平台券
     */
    private Integer source;

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
     * 有效期开始时间
     */
    private Date validStartTime;

    /**
     * 有效期结束时间
     */
    private Date validEndTime;

    /**
     * 库存
     */
    private Integer stock;

    /**
     * 领取规则
     */
    private String receiveRule;

    /**
     * 消耗规则
     */
    private String consumeRule;

    /**
     * 优惠券状态 0：生效中 1：已结束
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 修改时间
     */
    private Date updateTime;

    /**
     * 删除标识 0：未删除 1：已删除
     */
    private Integer delFlag;
}