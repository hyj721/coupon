package com.uestc.onecoupon.merchant.admin.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.util.Date;

@Data
public class CouponTaskCreateReqDTO {

    /**
     * 优惠券批次任务名称
     */
    private String taskName;

    /**
     * 文件地址，例如"/Users/hyj/项目/java-project/coupon/tmp/oneCoupon任务推送Excel.xlsx"
     */
    private String fileAddress;

    /**
     * 通知方式 0：站内信 1：弹框推送 2：邮箱 3：短信。可组合使用，例如1,2
     */
    private String notifyType;

    /**
     * 优惠券模板id
     */
    private String couponTemplateId;

    /**
     * 发送类型 0：立即发送 1：定时发送
     */
    private Integer sendType;

    /**
     * 发送时间
     */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private Date sendTime;
}
