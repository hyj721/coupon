package com.uestc.onecoupon.engine.common.constant;

public final class EngineRedisConstant {

    /**
     * 优惠券模板缓存 Key
     */
    public static final String COUPON_TEMPLATE_KEY = "one-coupon_engine:template:%s:%s";

    /**
     * 优惠券模板缓存分布式锁 Key
     */
    public static final String LOCK_COUPON_TEMPLATE_KEY = "one-coupon_engine:lock:template:%s:%s";


    /**
     * 优惠券模板缓存空值 Key
     */
    public static final String COUPON_TEMPLATE_IS_NULL_KEY = "one-coupon_engine:template_is_null:%s:%s";
}