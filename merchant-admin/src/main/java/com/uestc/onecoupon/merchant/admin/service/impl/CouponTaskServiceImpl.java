package com.uestc.onecoupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.alibaba.fastjson2.JSONObject;
import com.uestc.coupon.framework.exception.ClientException;
import com.uestc.onecoupon.merchant.admin.common.context.UserContext;
import com.uestc.onecoupon.merchant.admin.common.enums.CouponTaskSendTypeEnum;
import com.uestc.onecoupon.merchant.admin.common.enums.CouponTaskStatusEnum;
import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTaskDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTaskMapper;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.mq.event.CouponTaskExecuteEvent;
import com.uestc.onecoupon.merchant.admin.mq.producer.CouponTaskActualExecuteProducer;
import com.uestc.onecoupon.merchant.admin.service.ICouponTaskService;
import com.uestc.onecoupon.merchant.admin.service.ICouponTemplateService;
import com.uestc.onecoupon.merchant.admin.service.handler.excel.RowCountListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.redisson.api.RBlockingDeque;
import org.redisson.api.RDelayedQueue;
import org.redisson.api.RedissonClient;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponTaskServiceImpl implements ICouponTaskService {

    private final ICouponTemplateService couponTemplateService;

    private final CouponTaskMapper couponTaskMapper;

    private final RedissonClient redissonClient;

    private final CouponTaskActualExecuteProducer couponTaskActualExecuteProducer;


    private final ExecutorService executorService = new ThreadPoolExecutor(
            Runtime.getRuntime().availableProcessors(),
            Runtime.getRuntime().availableProcessors() << 1,
            60,
            TimeUnit.SECONDS,
            new SynchronousQueue<>(),
            new ThreadPoolExecutor.DiscardPolicy()
    );

    /**
     * 商家创建优惠券推送任务
     * 在方法上加了个 @Transactional(rollbackFor = Exception.class) 注解，
     * 这是因为如果不加注解的话，我们执行数据库插入操作成功了，但是线程池和延时队列都没有执行。这种情况下，发送条数数据就永远不会被刷新。
     * @param requestParam 请求参数
     */
    @Transactional(rollbackFor = Exception.class)
    @Override
    public void createCouponTask(CouponTaskCreateReqDTO requestParam) {
        CouponTemplateQueryRespDTO couponTemplate = couponTemplateService.findCouponTemplateById(requestParam.getCouponTemplateId());
        if (couponTemplate == null) {
            throw new ClientException("优惠券模版不存在,请检查提交信息是否正确");
        }
        CouponTaskDO couponTaskDO = BeanUtil.copyProperties(requestParam, CouponTaskDO.class);
        couponTaskDO.setShopNumber(UserContext.getShopNumber());
        couponTaskDO.setBatchId(IdUtil.getSnowflakeNextId());
        couponTaskDO.setStatus(
                Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())
                        ? CouponTaskStatusEnum.IN_PROGRESS.getStatus()
                        : CouponTaskStatusEnum.PENDING.getStatus()
        );
        couponTaskDO.setOperatorId(Long.parseLong(UserContext.getUserId()));

        // 先插入数据，再更新sendNum
        couponTaskMapper.insert(couponTaskDO);

        // 为什么需要统计行数？因为发送后需要比对所有优惠券是否都已发放到用户账号
        // 100 万数据大概需要 4 秒才能返回前端，如果加上验证将会时间更长，所以这里将最耗时的统计操作异步化
        JSONObject delayJsonObject = JSONObject
                .of("fileAddress", requestParam.getFileAddress(), "couponTaskId", couponTaskDO.getId());
        executorService.execute(() -> {
            refreshCouponTaskSendNum(delayJsonObject);
        });

        // 假设刚把消息提交到线程池，突然应用宕机了，我们通过延迟队列进行兜底 Refresh
        RBlockingDeque<Object> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
        RDelayedQueue<Object> delayedQueue = redissonClient.getDelayedQueue(blockingDeque);
        // 这里延迟时间设置 20 秒，原因是我们笃定上面线程池 20 秒之内就能结束任务
        delayedQueue.offer(delayJsonObject, 20, TimeUnit.SECONDS);

        // 如果是立即发送任务，直接调用消息队列进行发送流程
        if (Objects.equals(requestParam.getSendType(), CouponTaskSendTypeEnum.IMMEDIATE.getType())) {
            // 执行优惠券推送业务，正式向用户发放优惠券
            CouponTaskExecuteEvent couponTaskExecuteEvent = CouponTaskExecuteEvent.builder()
                    .couponTaskId(couponTaskDO.getId())
                    .build();
            couponTaskActualExecuteProducer.sendMessage(couponTaskExecuteEvent);
        }

    }

    private void refreshCouponTaskSendNum(JSONObject delayJsonObject) {
        // 通过 EasyExcel 监听器获取 Excel 中所有行数
        RowCountListener listener = new RowCountListener();
        EasyExcel.read(delayJsonObject.getString("fileAddress"), listener).sheet().doRead();
        int totalRows = listener.getRowCount();
        // 刷新优惠券推送记录中发送行数
        CouponTaskDO updateCouponTaskDO = CouponTaskDO.builder()
                .id(delayJsonObject.getLong("couponTaskId"))
                .sendNum(totalRows)
                .build();
        couponTaskMapper.updateById(updateCouponTaskDO);
    }

    @Service
    @RequiredArgsConstructor
    class RefreshCouponTaskDelayQueueRunner implements CommandLineRunner {

        private final CouponTaskMapper couponTaskMapper;
        private final RedissonClient redissonClient;

        @Override
        public void run(String... args) throws Exception {
            Executors.newSingleThreadExecutor(
                            runnable -> {
                                Thread thread = new Thread(runnable);
                                thread.setName("delay_coupon-task_send-num_consumer");
                                thread.setDaemon(Boolean.TRUE);
                                return thread;
                            })
                    .execute(() -> {
                        RBlockingDeque<JSONObject> blockingDeque = redissonClient.getBlockingDeque("COUPON_TASK_SEND_NUM_DELAY_QUEUE");
                        for (; ; ) {
                            try {
                                // 获取延迟队列已到达时间元素
                                JSONObject delayJsonObject = blockingDeque.take();
                                if (delayJsonObject != null) {
                                    // 获取优惠券推送记录，查看发送条数是否已经有值，有的话代表上面线程池已经处理完成，无需再处理
                                    CouponTaskDO couponTaskDO = couponTaskMapper.selectById(delayJsonObject.getLong("couponTaskId"));
                                    if (couponTaskDO.getSendNum() == null) {
                                        refreshCouponTaskSendNum(delayJsonObject);
                                    }
                                }
                            } catch (Throwable ignored) {
                            }
                        }
                    });
        }
    }
}
