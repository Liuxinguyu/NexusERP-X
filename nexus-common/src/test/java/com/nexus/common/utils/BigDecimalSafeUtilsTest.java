package com.nexus.common.utils;

import com.nexus.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BigDecimalSafeUtilsTest {

    @Test
    void safeDivideMoney_shouldKeepTwoDecimals() {
        BigDecimal result = BigDecimalSafeUtils.safeDivideMoney(
                new BigDecimal("10"),
                new BigDecimal("3")
        );
        assertThat(result).isEqualByComparingTo("3.33");
    }

    @Test
    void safeDivideRatio_shouldKeepFourDecimals() {
        BigDecimal result = BigDecimalSafeUtils.safeDivideRatio(
                new BigDecimal("1"),
                new BigDecimal("3")
        );
        assertThat(result).isEqualByComparingTo("0.3333");
    }

    @Test
    void safeDivide_shouldThrowWhenDivisorIsZero() {
        assertThatThrownBy(() -> BigDecimalSafeUtils.safeDivideMoney(
                new BigDecimal("10"),
                BigDecimal.ZERO
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("计算失败：除数不能为零或空");
    }

    @Test
    void safeDivide_shouldThrowWhenOperandIsNull() {
        assertThatThrownBy(() -> BigDecimalSafeUtils.safeDivideRatio(
                null,
                new BigDecimal("2")
        ))
                .isInstanceOf(BusinessException.class)
                .hasMessage("计算失败：除数不能为零或空");
    }
}
