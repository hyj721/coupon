package com.uestc.onecoupon.merchant.admin.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.util.IdUtil;
import com.alibaba.excel.EasyExcel;
import com.uestc.coupon.framework.exception.ClientException;
import com.uestc.onecoupon.merchant.admin.common.context.UserContext;
import com.uestc.onecoupon.merchant.admin.common.enums.CouponTaskSendTypeEnum;
import com.uestc.onecoupon.merchant.admin.common.enums.CouponTaskStatusEnum;
import com.uestc.onecoupon.merchant.admin.dao.entity.CouponTaskDO;
import com.uestc.onecoupon.merchant.admin.dao.mapper.CouponTaskMapper;
import com.uestc.onecoupon.merchant.admin.dto.req.CouponTaskCreateReqDTO;
import com.uestc.onecoupon.merchant.admin.dto.resp.CouponTemplateQueryRespDTO;
import com.uestc.onecoupon.merchant.admin.service.ICouponTaskService;
import com.uestc.onecoupon.merchant.admin.service.ICouponTemplateService;
import com.uestc.onecoupon.merchant.admin.service.handler.excel.RowCountListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Objects;

@Service
@RequiredArgsConstructor
@Slf4j
public class CouponTaskServiceImpl implements ICouponTaskService {

    private final ICouponTemplateService couponTemplateService;

    private final CouponTaskMapper couponTaskMapper;

    /**
     * 商家创建优惠券推送任务
     *
     * @param requestParam 请求参数
     */
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

        // 通过 EasyExcel 监听器获取 Excel 中所有行数
        RowCountListener listener = new RowCountListener();
        EasyExcel.read(requestParam.getFileAddress(), listener).sheet().doRead();

        // 为什么需要统计行数？因为发送后需要比对所有优惠券是否都已发放到用户账号
        int totalRows = listener.getRowCount();
        couponTaskDO.setSendNum(totalRows);
        couponTaskDO.setCompletionTime(new Date());

        // 保存优惠券推送任务记录到数据库
        couponTaskMapper.insert(couponTaskDO);
    }
}
