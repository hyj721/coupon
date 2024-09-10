package com.uestc.onecoupon.merchant.admin.dao.sharding;

import lombok.Getter;
import org.apache.shardingsphere.infra.util.exception.ShardingSpherePreconditions;
import org.apache.shardingsphere.sharding.api.sharding.standard.PreciseShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.RangeShardingValue;
import org.apache.shardingsphere.sharding.api.sharding.standard.StandardShardingAlgorithm;
import org.apache.shardingsphere.sharding.exception.algorithm.sharding.ShardingAlgorithmInitializationException;

import java.util.Collection;
import java.util.List;
import java.util.Properties;

public final class DBHashModShardingAlgorithm implements StandardShardingAlgorithm<Long> {

    @Getter
    private Properties props;

    // 这是所有分片的总数（即所有表的总数），在初始化时从配置文件中获取。
    private int shardingCount;
    private static final String SHARDING_COUNT_KEY = "sharding-count";

    /**
     *
     * @param availableTargetNames 这是数据库的集合。比如，我们有两个数据库 ds_0 和 ds_1，这些就是可能的目标数据库。
     * @param shardingValue 这是分片键的值，比如 shop_number，它决定数据应该分配到哪个数据库。
     */
    @Override
    public String doSharding(Collection<String> availableTargetNames, PreciseShardingValue<Long> shardingValue) {
        long id = shardingValue.getValue();
        int dbSize = availableTargetNames.size();
        int mod = (int) hashShardingValue(id) % shardingCount / (shardingCount / dbSize);
        int index = 0;
        for (String targetName : availableTargetNames) {
            if (index == mod) {
                System.out.println("targetName: " + targetName);
                return targetName;
            }
            index++;
        }
        throw new IllegalArgumentException("No target found for value: " + id);
    }

    @Override
    public Collection<String> doSharding(Collection<String> availableTargetNames, RangeShardingValue<Long> shardingValue) {
        // 暂无范围分片场景，默认返回空
        return List.of();
    }

    @Override
    public void init(Properties props) {
        this.props = props;
        shardingCount = getShardingCount(props);
    }

    private int getShardingCount(final Properties props) {
        ShardingSpherePreconditions.checkState(props.containsKey(SHARDING_COUNT_KEY), () -> new ShardingAlgorithmInitializationException(getType(), "Sharding count cannot be null."));
        return Integer.parseInt(props.getProperty(SHARDING_COUNT_KEY));
    }

    private long hashShardingValue(final Comparable<?> shardingValue) {
        return Math.abs((long) shardingValue.hashCode());
    }
}