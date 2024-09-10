package com.uestc.onecoupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.mzt.logapi.context.LogRecordContext;
import com.mzt.logapi.starter.annotation.LogRecord;
import com.uestc.onecoupon.merchant.admin.common.constant.MerchantAdminRedisConstant;
import com.uestc.onecoupon.merchant.admin.common.context.UserContext;
import com.uestc.onecoupon.merchant.admin.common.enums.CouponTemplateStatusEnum;
import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTemplateDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTemplateMapper;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTemplateSaveReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.service.ICouponTemplateService;
import com.uestc.onecoupon.merchant.admin.service.basics.chain.MerchantAdminChainContext;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.uestc.onecoupon.merchant.admin.common.enums.ChainBizMarkEnum.MERCHANT_ADMIN_CREATE_COUPON_TEMPLATE_KEY;


@Service
@RequiredArgsConstructor
public class CouponTemplateServiceImpl implements ICouponTemplateService {

    private final CouponTemplateMapper couponTemplateMapper;

    private final MerchantAdminChainContext merchantAdminChainContext;

    private final StringRedisTemplate stringRedisTemplate;

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
    }
}
