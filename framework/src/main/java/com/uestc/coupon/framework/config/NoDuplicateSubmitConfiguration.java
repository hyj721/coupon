package com.uestc.coupon.framework.config;

import com.uestc.coupon.framework.idempotent.NoDuplicateSubmitAspect;
import org.redisson.api.RedissonClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class NoDuplicateSubmitConfiguration {
    @Bean
    public NoDuplicateSubmitAspect noDuplicateSubmit(RedissonClient redissonClient) {
        return new NoDuplicateSubmitAspect(redissonClient);
    }
}
