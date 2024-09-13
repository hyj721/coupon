package com.uestc.onecoupon.merchant.admin.mq.consumer;

import com.alibaba.fastjson2.JSONObject;
import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.uestc.onecoupon.merchant.admin.mq.base.MessageWrapper;
import com.uestc.onecoupon.merchant.admin.mq.event.CouponTemplateDelayEvent;
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
public class CouponTemplateDelayExecuteStatusConsumer implements RocketMQListener<MessageWrapper<CouponTemplateDelayEvent>> {
    private final CouponTemplateMapper couponTemplateMapper;

    @Override
    public void onMessage(MessageWrapper<CouponTemplateDelayEvent> messageWrapper) {
        log.info("[消费者] 优惠券模板定时执行@变更模板表状态 - 执行消费逻辑，消息体：{}", JSONObject.toJSONString(messageWrapper));

        // 变更消费券状态为结束，只需要处理数据库即可，因为redis设置了过期时间
        CouponTemplateDelayEvent message = messageWrapper.getMessage();
        if (message == null) {
            log.error("[消费者] 优惠券模板定时执行@变更模板表状态 - 消息体为空，不执行消费逻辑");
            return;
        }
        CouponTemplateDO couponTemplateDOReq = CouponTemplateDO.builder()
                .shopNumber(message.getShopNumber())
                .id(message.getCouponTemplateId())
                .build();
        couponTemplateMapper.updateCouponTemplateEnd(couponTemplateDOReq);
    }
}
