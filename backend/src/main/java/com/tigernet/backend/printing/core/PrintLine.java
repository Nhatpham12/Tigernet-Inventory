package com.tigernet.backend.printing.core;

import com.tigernet.backend.catalog.entity.ProductEntity;

import java.math.BigDecimal;

/**
 * Một dòng hàng trên biểu mẫu, đã đọc sẵn từ entity để template không chạm lazy-loading.
 * Trường nào không dùng thì để null.
 */
public record PrintLine(
        int stt,
        String name,
        String code,
        String unit,
        String serial,
        Integer quantity,
        Integer quantityActual,  // thực nhập / thực xuất / thực tế (kiểm kê)
        BigDecimal unitPrice,
        BigDecimal amount,
        Integer warrantyMonths,
        String note
) {
    /** Mã hàng in trên phiếu: DB v7.2 chưa có SKU nên dùng mã nội bộ SP + ID (VD SP0012). */
    public static String productCode(ProductEntity product) {
        return product == null || product.getId() == null ? "" : String.format("SP%04d", product.getId());
    }
}
