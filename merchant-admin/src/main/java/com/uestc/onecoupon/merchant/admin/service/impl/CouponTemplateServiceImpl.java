package com.uestc.onecoupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.ObjectUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.uestc.coupon.framework.exception.ClientException;
import com.uestc.coupon.framework.exception.ServiceException;
import com.uestc.onecoupon.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.uestc.onecoupon.merchant.admin.common.context.UserContext;
import com.uestc.onecoupon.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateNumberReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplatePageQueryReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplatePageQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.mq.event.CouponTemplateDelayEvent;
import com.uestc.onecoupon.merchant.admin.mq.producer.CouponTemplateDelayExecuteStatusProducer;
import com.uestc.onecoupon.merchant.admin.service.ICouponTemplateService;
import com.uestc.onecoupon.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.common.message.MessageConst;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.messaging.Message;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

import static com.uestc.onecoupon.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;

@Slf4j
@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl implements ICouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;

    private final MerchantAdminChainContext merchantAdminChainContext;

    private final StringRedisTemplate stringRedisTemplate;

    private final ConfigurableEnvironment configurableEnvironment;

    private final CouponTemplateDelayExecuteStatusProducer couponTemplateDelayExecuteStatusProducer;



    @LogRecord(
            success = """
                    创建优惠券：{{#requestParam.name}}， \
                    优惠对象：{COMMON_ENUM_PARSE{'DiscountTargetEnum' + '_' + #requestParam.target}}， \
                    优惠类型：{COMMON_ENUM_PARSE{'DiscountTypeEnum' + '_' + #requestParam.type}}， \
                    库存数量：{{#requestParam.stock}}， \
                    优惠商品编码：{{#requestParam.goods}}， \
                    有效期开始时间：{{#requestParam.validStartTime}}， \
                    有效期结束时间：{{#requestParam.validEndTime}}， \
                    领取规则：{{#requestParam.receiveRule}}， \
                    消耗规则：{{#requestParam.consumeRule}};
                    """,
            type = "CouponTemplate", // 指定该日志的业务类型是 CouponTemplate，表示这条日志的记录类型。
            bizNo = "{{#bizNo}}",
            extra = "{{#requestParam.toString()}}"
    )
    @Override
    public void createCouponTemplate(CouponTemplateSaveReqDTO requestParam) {
        // 通过责任链验证请求参数是否正确，如果没通过会抛出异常
        merchantAdminChainContext.handler(MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY.name(), requestParam);

        // 新增优惠券模板信息到数据库
        CouponTemplateDO couponTemplateDO = BeanUtil.toBean(requestParam, CouponTemplateDO.class);
        couponTemplateDO.setStatus(CouponTemplateStatusEnum.ACTIVE.getStatus());
        couponTemplateDO.setShopNumber(UserContext.getShopNumber());
        couponTemplateMapper.insert(couponTemplateDO);
        LogRecordContext.putVariable("bizNo", couponTemplateDO.getId());
        // 在此处暂时不put originalData，第一次创建就只记录modified_data，而不用冗余记录original_data，只有优惠券变化的时候才记录

        // 缓存预热：通过将数据库的记录序列化成 JSON 字符串放入 Redis 缓存
        CouponTemplateQueryRespDTO actualRespDTO = BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
        Map<String, Object> cacheTargetMap = BeanUtil.beanToMap(actualRespDTO, false, true);

        // 创建一个新的 Map<String, String>，用于存放转换后的键值对，原来的模版是 Map<String, Object>
        Map<String, String> actualCacheTargetMap = cacheTargetMap.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> entry.getValue() != null ? entry.getValue().toString() : ""
                ));
        // one-coupon_engine:template:实际id
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateDO.getId());

        // 通过 LUA 脚本执行设置 Hash 数据以及设置过期时间
        String luaScript = "redis.call('HMSET', KEYS[1], unpack(ARGV, 1, #ARGV - 1)) " +
                "redis.call('EXPIREAT', KEYS[1], ARGV[#ARGV])";

        List<String> keys = Collections.singletonList(couponTemplateCacheKey);
        List<String> args = new ArrayList<>(actualCacheTargetMap.size() * 2 + 1);
        actualCacheTargetMap.forEach((key, value) -> {
            args.add(key);
            args.add(value);
        });

        // 优惠券活动过期时间转换为秒级别的 Unix 时间戳
        args.add(String.valueOf(couponTemplateDO.getValidEndTime().getTime() / 1000));

        // 执行 LUA 脚本
        stringRedisTemplate.execute(
                new DefaultRedisScript<>(luaScript, Long.class),
                keys,
                args.toArray()
        );

        String couponTemplateDelayCloseTopic = "one-coupon_merchant-admin-service_coupon-template-delay_topic${unique-name:}";
        // 通过 Spring 上下文解析占位符，也就是把 VM 参数里的 unique-name 替换到字符串中
        couponTemplateDelayCloseTopic = configurableEnvironment.resolvePlaceholders(couponTemplateDelayCloseTopic);

        // 定义消息体
        JSONObject messageBody = new JSONObject();
        messageBody.put("couponTemplateId", couponTemplateDO.getId());
        messageBody.put("shopNumber", UserContext.getShopNumber());

        // 设置消息的送达时间，毫秒级 Unix 时间戳
        Long deliverTimeStamp = couponTemplateDO.getValidEndTime().getTime();

        // 构建消息体
        String messageKeys = UUID.randomUUID().toString();
        Message<JSONObject> message = MessageBuilder
                .withPayload(messageBody)
                .setHeader(MessageConst.PROPERTY_KEYS, messageKeys)
                .build();

        // 执行 RocketMQ5.x 消息队列发送&异常处理逻辑
        // 消息ID是由 RocketMQ 在消息发送时自动生成的，通常是由 RocketMQ 的 Broker 系统生成的一个唯一标识符，用来标识每条消息。
        // 消息Keys是由应用程序在发送消息时手动设置的，可以是具有业务意义的唯一标识符。例如，在电商系统中，订单ID可以作为消息的Keys。
        // 发送延时消息事件，优惠券活动到期修改优惠券模板状态
        CouponTemplateDelayEvent templateDelayEvent = CouponTemplateDelayEvent.builder()
                .shopNumber(UserContext.getShopNumber())
                .couponTemplateId(couponTemplateDO.getId())
                .delayTime(couponTemplateDO.getValidEndTime().getTime())
                .build();
        couponTemplateDelayExecuteStatusProducer.sendMessage(templateDelayEvent);


    }

    @Override
    public PageInfo<CouponTemplatePageQueryRespDTO> pageQueryCouponTemplate(CouponTemplatePageQueryReqDTO requestParam) {
        if (requestParam.getCurrent() == null || requestParam.getCurrent() <= 0
                || requestParam.getSize() == null || requestParam.getSize() <= 0) {
            throw new ClientException("分页查询输入参数非法");
        }
        requestParam.setShopNumber(UserContext.getShopNumber());
        PageHelper.startPage(requestParam.getCurrent(), requestParam.getSize());
        List<CouponTemplatePageQueryRespDTO> selectPage = couponTemplateMapper.queryPage(requestParam);
        return new PageInfo<>(selectPage);
    }

    @Override
    public CouponTemplateQueryRespDTO findCouponTemplateById(String couponTemplateId) {
        Long shopNumber = UserContext.getShopNumber();
        CouponTemplateDO couponTemplateDOReq = CouponTemplateDO.builder()
                .shopNumber(shopNumber)
                .id(Long.parseLong(couponTemplateId))
                .build();
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.query(couponTemplateDOReq);
        return BeanUtil.toBean(couponTemplateDO, CouponTemplateQueryRespDTO.class);
    }

    @LogRecord(
            success = "增加发行量：{{#requestParam.number}}",
            type = "CouponTemplate",
            bizNo = "{{#requestParam.couponTemplateId}}"
    )
    @Override
    public void increaseNumberCouponTemplate(CouponTemplateNumberReqDTO requestParam) {
        Long shopNumber = UserContext.getShopNumber();
        CouponTemplateDO couponTemplateDOReq = CouponTemplateDO.builder()
                .shopNumber(shopNumber)
                .id(Long.parseLong(requestParam.getCouponTemplateId()))
                .build();
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.query(couponTemplateDOReq);
        if (couponTemplateDO == null) {
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }

        // 验证优惠券模板是否正常
        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已结束");
        }
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));
        int rows = couponTemplateMapper.increaseNumberCouponTemplate(couponTemplateDOReq, requestParam.getNumber());
        if (rows != 1) {
            throw new ServiceException("优惠券模板增加发行量失败");
        }
        // 增加优惠券模板缓存库存发行量
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, requestParam.getCouponTemplateId());
        stringRedisTemplate.opsForHash().increment(couponTemplateCacheKey, "stock", requestParam.getNumber());
    }

    @LogRecord(
            success = "结束优惠券",
            type = "CouponTemplate",
            bizNo = "{{#couponTemplateId}}"
    )
    @Override
    public void terminateCouponTemplate(String couponTemplateId) {
        Long shopNumber = UserContext.getShopNumber();
        CouponTemplateDO couponTemplateDOReq = CouponTemplateDO.builder()
                .shopNumber(shopNumber)
                .id(Long.parseLong(couponTemplateId))
                .build();
        CouponTemplateDO couponTemplateDO = couponTemplateMapper.query(couponTemplateDOReq);
        if (couponTemplateDO == null) {
            // 一旦查询优惠券不存在，基本可判定横向越权，可上报该异常行为，次数多了后执行封号等处理
            throw new ClientException("优惠券模板异常，请检查操作是否正确...");
        }
        // 验证优惠券模板是否正常
        if (ObjectUtil.notEqual(couponTemplateDO.getStatus(), CouponTemplateStatusEnum.ACTIVE.getStatus())) {
            throw new ClientException("优惠券模板已结束");
        }
        // 记录优惠券模板修改前数据
        LogRecordContext.putVariable("originalData", JSON.toJSONString(couponTemplateDO));
        // 修改优惠券模板为结束状态
        couponTemplateMapper.updateCouponTemplateEnd(couponTemplateDOReq);

        // 修改优惠券模板缓存状态为结束状态
        String couponTemplateCacheKey = String.format(MerchantAdminRedisConstant.COUPON_TEMPLATE_KEY, couponTemplateId);
        stringRedisTemplate.opsForHash().put(couponTemplateCacheKey, "status", String.valueOf(CouponTemplateStatusEnum.ENDED.getStatus()));
    }
}
