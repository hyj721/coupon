package com.uestc.onecoupon.merchant.admin.mq.producer;

import cn.hutool.core.util.StrUtil;
import com.uestc.onecoupon.merchant.admin.mq.base.BaseSendExtendDTO;
import com.uestc.onecoupon.merchant.admin.mq.base.MessageWrapper;
import com.uestc.onecoupon.merchant.admin.mq.event.CouponTaskExecuteEvent;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * 优惠券推送任务执行生产者
 * <p>
 * 作者：马丁
 * 加项目群：早加入就是优势！500人内部项目群，分享的知识总有你需要的 <a href="https://t.zsxq.com/cw7b9" />
 * 开发时间：2024-07-23
 */
@Slf4j
@Component
public class CouponTaskActualExecuteProducer extends AbstractCommonSendProduceTemplate<CouponTaskExecuteEvent> {

    private final ConfigurableEnvironment environment;

    public CouponTaskActualExecuteProducer(@Autowired RocketMQTemplate rocketMQTemplate, @Autowired ConfigurableEnvironment environment) {
        super(rocketMQTemplate);
        this.environment = environment;
    }

    @Override
    protected BaseSendExtendDTO buildBaseSendExtendParam(CouponTaskExecuteEvent messageSendEvent) {
        return BaseSendExtendDTO.builder()
                .eventName("优惠券推送执行")
                .topic(environment.resolvePlaceholders("one-coupon_distribution-service_coupon-task-execute_topic${unique-name:}"))
                .keys(String.valueOf(messageSendEvent.getCouponTaskId()))
                .sentTimeout(2000L)
                .build();
    }

    @Override
    protected Message<?> buildMessage(CouponTaskExecuteEvent messageSendEvent, BaseSendExtendDTO requestParam) {
        String keys = StrUtil.isEmpty(requestParam.getKeys()) ? UUID.randomUUID().toString() : requestParam.getKeys();
        Message<MessageWrapper> build = MessageBuilder
                .withPayload(new MessageWrapper(keys, messageSendEvent))
                .setHeader(MessageConst.PROPERTY_KEYS, keys)
                .setHeader(MessageConst.PROPERTY_TAGS, requestParam.getTag())
                .build();
        return build;
    }
}
