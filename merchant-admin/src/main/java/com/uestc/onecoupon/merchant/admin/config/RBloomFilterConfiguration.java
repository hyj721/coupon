package com.uestc.onecoupon.merchant.admin.config;

import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 布隆过滤器配置类
 */
@Configuration
public class RBloomFilterConfiguration {
    /**
     * 优惠券查询缓存穿透布隆过滤器
     */
    @Bean
    public RBloomFilter<String> couponTemplateQueryBloomFilter(RedissonClient redissonClient) {
        RBloomFilter<String> bloomFilter = redissonClient.getBloomFilter("couponTemplateQueryBloomFilter");
        // 布隆过滤器的预计数据量640L， 布隆过滤器的误判率0.1%
        bloomFilter.tryInit(640L, 0.001);
        return bloomFilter;
    }
}
