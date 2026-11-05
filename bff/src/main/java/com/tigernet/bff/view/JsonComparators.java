package com.tigernet.bff.view;

import tools.jackson.databind.JsonNode;

import java.util.Comparator;

final class JsonComparators {
    private JsonComparators() {
    }

    /** so sánh theo một trường của JsonNode: số so theo giá trị, còn lại so chuỗi (ngày ISO so chuỗi vẫn đúng) */
    static Comparator<JsonNode> byField(String field) {
        return (a, b) -> {
            JsonNode x = a.get(field);
            JsonNode y = b.get(field);
            boolean xNull = x == null || x.isNull();
            boolean yNull = y == null || y.isNull();
            if (xNull || yNull) {
                return Boolean.compare(xNull, yNull);
            }
            if (x.isNumber() && y.isNumber()) {
                return x.decimalValue().compareTo(y.decimalValue());
            }
            return x.asString().compareToIgnoreCase(y.asString());
        };
    }
}
