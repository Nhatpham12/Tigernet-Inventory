package com.tigernet.backend.common.util;

import java.time.Year;

// số phiếu theo mẫu {Loại}-{Warehouse_Code}-{yyyy}-{0001}, VD PN-MAIN-2026-0001
public final class DocumentCodeGenerator {
    private DocumentCodeGenerator() {
    }

    public static String prefix(String type, String warehouseCode) {
        return type + "-" + warehouseCode + "-" + Year.now().getValue() + "-";
    }

    // lastCode: số phiếu lớn nhất đang có cùng prefix (null nếu chưa có)
    public static String next(String prefix, String lastCode) {
        int next = 1;
        if (lastCode != null) {
            next = Integer.parseInt(lastCode.substring(prefix.length())) + 1;
        }
        return prefix + String.format("%04d", next);
    }
}
