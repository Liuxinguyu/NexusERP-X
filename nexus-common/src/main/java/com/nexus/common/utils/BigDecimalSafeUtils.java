package com.nexus.common.utils;

import com.nexus.common.core.domain.ResultCode;
import com.nexus.common.exception.BusinessException;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * BigDecimal 安全除法工具，统一防止空指针与除零异常。
 */
public final class BigDecimalSafeUtils {

    private static final String DIVIDE_ERROR_MSG = "计算失败：除数不能为零或空";

    private BigDecimalSafeUtils() {
    }

    /**
     * 金额除法：固定保留 2 位小数，四舍五入。
     */
    public static BigDecimal safeDivideMoney(BigDecimal dividend, BigDecimal divisor) {
        validateOperands(dividend, divisor);
        return dividend.divide(divisor, 2, RoundingMode.HALF_UP);
    }

    /**
     * 比例/汇率除法：固定保留 4 位小数，四舍五入。
     */
    public static BigDecimal safeDivideRatio(BigDecimal dividend, BigDecimal divisor) {
        validateOperands(dividend, divisor);
        return dividend.divide(divisor, 4, RoundingMode.HALF_UP);
    }

    /**
     * 这里选择“抛业务异常”而不是“返回 0”，避免静默吞掉计算错误导致财务数据失真。
     */
    private static void validateOperands(BigDecimal dividend, BigDecimal divisor) {
        if (dividend == null || divisor == null || divisor.compareTo(BigDecimal.ZERO) == 0) {
            throw new BusinessException(ResultCode.BAD_REQUEST, DIVIDE_ERROR_MSG);
        }
    }
}
