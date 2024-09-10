package com.uestc.onecoupon.merchant.admin.service.basics.chain;

import org.springframework.beans.BeansException;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import java.util.*;

/**
 * 过滤链上下文类，可以根据mark组织责任链，并提供了handler方法
 * @param <T>
 */
@Component
public final class MerchantAdminChainContext<T> implements ApplicationContextAware, CommandLineRunner {

    /**
     * 应用上下文，我们这里通过 Spring IOC 获取 Bean 实例
     */
    private ApplicationContext applicationContext;

    private final Map<String, List<MerchantAdminAbstractChainHandler>> abstractChainHandlerContainer = new HashMap<>();

    public void handler(String mark, T requestParam) {
        // 根据 mark 标识从责任链容器中获取一组责任链实现 Bean 集合
        List<MerchantAdminAbstractChainHandler> abstractChainHandlers = abstractChainHandlerContainer.get(mark);
        if (CollectionUtils.isEmpty(abstractChainHandlers)) {
            throw new RuntimeException(String.format("[%s] Chain of Responsibility ID is undefined.", mark));
        }
        abstractChainHandlers.forEach(each -> each.handler(requestParam));
    }

    @Override
    public void run(String... args) throws Exception {
        // 暂时只有一个mark
        // 3个不同的过滤链，mark相同，但是容器内的名字不同
        Map<String, MerchantAdminAbstractChainHandler> chainFilterMap = applicationContext.getBeansOfType(MerchantAdminAbstractChainHandler.class);
        List<MerchantAdminAbstractChainHandler> abstractChainHandlers = new ArrayList<>();
        for (MerchantAdminAbstractChainHandler chainFilter : chainFilterMap.values()) {
            abstractChainHandlers.add(chainFilter);
            abstractChainHandlerContainer.put(chainFilter.mark(), abstractChainHandlers);
        }
        abstractChainHandlerContainer.forEach((mark, unsortedChainHandlers) -> {
            // 对每个 Mark 对应的责任链实现类集合进行排序，优先级小的在前。暂时我们只有一个mark
            unsortedChainHandlers.sort(Comparator.comparing(Ordered::getOrder));
        });

    }

    @Override
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.applicationContext = applicationContext;
    }
}