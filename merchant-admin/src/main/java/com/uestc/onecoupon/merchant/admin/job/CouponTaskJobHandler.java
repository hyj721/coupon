package com.uestc.onecoupon.merchant.admin.job;

import cn.hutool.core.collection.CollUtil;
import com.uestc.onecoupon.merchant.admin.common.enums.CouponTaskStatusEnum;
import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTaskDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTaskMapper;
import com.uestc.onecoupon.merchant.admin.mq.event.CouponTaskExecuteEvent;
import com.uestc.onecoupon.merchant.admin.mq.producer.CouponTaskActualExecuteProducer;
import com.xxl.job.core.handler.IJobHandler;
import com.xxl.job.core.handler.annotation.XxlJob;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class CouponTaskJobHandler extends IJobHandler {

    private final CouponTaskMapper couponTaskMapper;

    private final CouponTaskActualExecuteProducer couponTaskActualExecuteProducer;

    private static final int MAX_LIMIT = 100;

    private int executeCount = 0;

    @XxlJob(value = "couponTemplateTask")
    public void execute() throws Exception {
        executeCount++;
        log.info("CouponTaskJobHandler执行次数: {}", executeCount);
        long initId = 0;

        while (true) {
            // 获取已到执行时间待执行的优惠券定时分发任务
            List<CouponTaskDO> couponTaskDOList = fetchPendingTasks(initId);

            if (CollUtil.isEmpty(couponTaskDOList)) {
                break;
            }

            // 调用分发服务对用户发送优惠券
            for (CouponTaskDO each : couponTaskDOList) {
                distributeCoupon(each);
            }

            // 查询出来的数据如果小于 MAX_LIMIT 意味着后面将不再有数据，返回即可
            if (couponTaskDOList.size() < MAX_LIMIT) {
                break;
            }

            // 更新 initId 为当前列表中最大 ID
            initId = couponTaskDOList.stream()
                    .mapToLong(CouponTaskDO::getId)
                    .max()
                    .orElse(initId);
        }
    }

    private void distributeCoupon(CouponTaskDO couponTask) {
        // 修改延时执行推送任务任务状态为执行中
        CouponTaskDO couponTaskDO = CouponTaskDO.builder()
                .id(couponTask.getId())
                .status(CouponTaskStatusEnum.IN_PROGRESS.getStatus())
                .build();
        couponTaskMapper.updateById(couponTaskDO);
        // 通过消息队列发送消息，由分发服务消费者消费该消息
        CouponTaskExecuteEvent couponTaskExecuteEvent = CouponTaskExecuteEvent.builder()
                .couponTaskId(couponTask.getId())
                .build();
        couponTaskActualExecuteProducer.sendMessage(couponTaskExecuteEvent);
    }

    private List<CouponTaskDO> fetchPendingTasks(long initId) {
        return couponTaskMapper.selectPendingTasks(initId, MAX_LIMIT);
    }
}
