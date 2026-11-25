package com.tigernet.backend.audit.service;

import tools.jackson.databind.json.JsonMapper;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Rút gọn kết quả API (DTO trả về cho FE) thành vài trường đủ để đọc nhật ký: mã phiếu, tên, kho, đối tác,
 * số tiền, trạng thái và tóm tắt dòng hàng. Chỉ giữ trường theo danh sách trắng và luôn bỏ trường nhạy cảm,
 * nên không lộ mật khẩu/token/SĐT/địa chỉ dù DTO có thêm trường mới.
 */
public final class AuditSummary {
    static final int MAX_ITEMS = 10;
    static final int MAX_TEXT = 200;
    static final int MAX_JSON = 4000;

    // trường giữ lại: id, mã (*Code), tên (*Name, trừ người thao tác *ByName vì đã có User_ID), tiền, số lượng, trạng thái, lý do/ghi chú
    private static final Pattern KEEP = Pattern.compile(
            "id|name|username|status|quantity|reason|note|.*Code|.*Name|.*Amount|.*Status|.*Reason");
    private static final Pattern BY_NAME = Pattern.compile(".*ByName");
    private static final Pattern SECRET = Pattern.compile("(?i).*(password|token|secret|mfa|otp|recovery|key).*");

    private AuditSummary() {}

    /** null khi không có gì đáng ghi */
    public static Map<String, Object> of(Object result, JsonMapper mapper) {
        if (result == null) {
            return null;
        }
        if (result instanceof Collection<?> list) {
            return Map.of("count", list.size());
        }
        Object value = mapper.convertValue(result, Object.class);
        if (!(value instanceof Map<?, ?> map)) {
            return null;
        }
        Map<String, Object> summary = new LinkedHashMap<>();
        map.forEach((k, v) -> {
            String key = String.valueOf(k);
            if (isScalar(v) && KEEP.matcher(key).matches() && !BY_NAME.matcher(key).matches()
                    && !SECRET.matcher(key).matches()) {
                summary.put(key, v instanceof String s && s.length() > MAX_TEXT ? s.substring(0, MAX_TEXT) + "…" : v);
            }
        });
        if (map.get("details") instanceof List<?> details) {
            summary.put("itemCount", details.size());
            List<String> items = new ArrayList<>();
            for (Object line : details.subList(0, Math.min(MAX_ITEMS, details.size()))) {
                if (line instanceof Map<?, ?> m) {
                    items.add(itemText(m));
                }
            }
            summary.put("items", items);
            if (mapper.writeValueAsString(summary).length() > MAX_JSON) {
                summary.remove("items"); // quá dài thì chỉ giữ số dòng
            }
        }
        return summary.isEmpty() ? null : summary;
    }

    // "Tên SP [serial] × SL"
    private static String itemText(Map<?, ?> line) {
        String name = String.valueOf(line.get("productName") != null ? line.get("productName") : line.get("productId"));
        if (name.length() > MAX_TEXT) {
            name = name.substring(0, MAX_TEXT) + "…";
        }
        Object serial = line.get("serial");
        Object quantity = line.get("quantity");
        return name + (serial != null ? " [" + serial + "]" : "") + (quantity != null ? " × " + quantity : "");
    }

    private static boolean isScalar(Object v) {
        return v instanceof String || v instanceof Number || v instanceof Boolean;
    }
}
