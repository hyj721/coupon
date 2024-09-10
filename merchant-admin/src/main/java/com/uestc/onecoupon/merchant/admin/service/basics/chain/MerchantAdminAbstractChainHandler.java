package com.uestc.onecoupon.merchant.admin.service.basics.chain;

import org.springframework.core.Ordered;

/**
 * 继承 Ordered 接口的主要用途是为了实现责任链中处理器的排序功能
 */
public interface MerchantAdminAbstractChainHandler<T> extends Ordered {

    /**
     * 执行责任链逻辑
     *
     * @param requestParam 责任链执行入参
     */
    void handler(T requestParam);

    /**
     * @return 责任链组件标识，三个实现类相同
     */
    String mark();
}