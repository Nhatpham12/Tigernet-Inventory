package com.tigernet.backend.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class MoneyUtils {
    private MoneyUtils() {
    }

    // làm tròn 2 chữ số như DECIMAL(18,2)
    public static BigDecimal round(BigDecimal value) {
        return value.setScale(2, RoundingMode.HALF_UP);
    }

    // thành tiền 1 dòng = số lượng × đơn giá
    public static BigDecimal lineTotal(Integer quantity, BigDecimal unitPrice) {
        return round(unitPrice.multiply(BigDecimal.valueOf(quantity)));
    }

    // value × percent / 100
    public static BigDecimal percentOf(BigDecimal value, BigDecimal percent) {
        return round(value.multiply(percent).divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP));
    }

    public static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
