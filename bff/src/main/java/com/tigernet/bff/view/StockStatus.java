package com.tigernet.bff.view;

// trạng thái tồn hiển thị bằng badge trên danh sách sản phẩm (Figma: Tồn thấp / Sắp hết / Bình thường)
public enum StockStatus {
    OUT_OF_STOCK, LOW, NEAR_LOW, NORMAL;

    public static StockStatus of(int available, Integer minStockLevel) {
        int min = minStockLevel == null ? 0 : minStockLevel;
        if (available <= 0) {
            return OUT_OF_STOCK;
        }
        if (available <= min) {
            return LOW;
        }
        // trong khoảng 150% ngưỡng tối thiểu thì báo "Sắp hết"
        if (available <= Math.ceil(min * 1.5)) {
            return NEAR_LOW;
        }
        return NORMAL;
    }
}
