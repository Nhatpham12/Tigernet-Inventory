package com.tigernet.bff.common;

import tools.jackson.databind.JsonNode;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Locale;

// đọc an toàn các trường của JsonNode trả về từ backend
public final class Json {
    private Json() {
    }

    public static String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asString();
    }

    public static Integer integer(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        return value == null || value.isNull() ? null : value.asInt();
    }

    public static int intOrZero(JsonNode node, String field) {
        Integer value = integer(node, field);
        return value == null ? 0 : value;
    }

    public static BigDecimal decimal(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.get(field);
        if (value == null || value.isNull()) {
            return BigDecimal.ZERO;
        }
        return value.isNumber() ? value.decimalValue() : new BigDecimal(value.asString());
    }

    public static LocalDateTime dateTime(JsonNode node, String field) {
        String value = text(node, field);
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.length() == 10 ? LocalDate.parse(value).atStartOfDay() : LocalDateTime.parse(value);
    }

    /** chuẩn hóa để tìm kiếm không phân biệt hoa/thường */
    public static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).trim();
    }
}
