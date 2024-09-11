package com.uestc.onecoupon.merchant.admin.mq.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTemplateMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.stereotype.Component;

@RocketMQMessageListener(
        topic = "one-coupon_merchant-admin-service_coupon-template-delay_topic${unique-name:}",
        consumerGroup = "one-coupon_merchant-admin-service_coupon-template-delay-status_cg${unique-name:}"
)
@Slf4j(topic = "CouponTemplateDelayExecuteStatusConsumer")
@Component
@RequiredArgsConstructor
public class CouponTemplateDelayExecuteStatusConsumer implements RocketMQListener<JSONObject> {
    private final CouponTemplateMapper couponTemplateMapper;

    @Override
    public void onMessage(JSONObject message) {
        log.info("[消费者] 优惠券模板定时执行@变更模板表状态 - 执行消费逻辑，消息体：{}", message.toString());

        // 变更消费券状态为结束，只需要处理数据库即可，因为redis设置了过期时间
        CouponTemplateDO couponTemplateDOReq = CouponTemplateDO.builder()
                .shopNumber(message.getLong("shopNumber"))
                .id(message.getLong("couponTemplateId"))
                .build();
        couponTemplateMapper.updateCouponTemplateEnd(couponTemplateDOReq);
    }
}
