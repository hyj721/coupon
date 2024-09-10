package com.uestc.onecoupon.merchant.admin.common.log;

import cn.hutool.core.util.StrUtil;
import com.mzt.logapi.service.IParseFunction;
import com.uestc.onecoupon.merchant.admin.common.enums.DiscountTargetEnum;
import com.uestc.onecoupon.merchant.admin.common.enums.DiscountTypeEnum;

import java.util.List;

public class CommonEnumParseFunction implements IParseFunction {

    // 获取折扣类型、折扣对象的枚举类类名，即DiscountTargetEnum和DiscountTypeEnum。
    public static final String DISCOUNT_TARGET_ENUM_NAME = DiscountTargetEnum.class.getSimpleName();
    private static final String DISCOUNT_TYPE_ENUM_NAME = DiscountTypeEnum.class.getSimpleName();


    /**
     * 使用该函数时的标识符。
     */
    @Override
    public String functionName() {
        return "COMMON_ENUM_PARSE";
    }

    @Override
    public String apply(Object value) {
        try {
            List<String> parts = StrUtil.split(value.toString(), "_");
            if (parts.size() != 2) {
                throw new IllegalArgumentException("格式错误，需要 '枚举类_具体值' 的形式。");
            }
            String enumClassName = parts.get(0);
            int enumValue = Integer.parseInt(parts.get(1));
            return findEnumValueByName(enumClassName, enumValue);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("第二个下划线后面的值需要是整数。", e);
        }
    }

    /**
     * 使用枚举类内的方法，根据 type 找到对应的 value。例如根据 0 找到 “商品专属优惠”
     */
    private String findEnumValueByName(String enumClassName, int enumValue) {
        if (DISCOUNT_TARGET_ENUM_NAME.equals(enumClassName)) {
            return DiscountTargetEnum.findValueByType(enumValue);
        } else if (DISCOUNT_TYPE_ENUM_NAME.equals(enumClassName)) {
            return DiscountTypeEnum.findValueByType(enumValue);
        } else {
            throw new IllegalArgumentException("未知的枚举类名: " + enumClassName);
        }
    }
}
