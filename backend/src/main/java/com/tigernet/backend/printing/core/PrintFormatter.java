package com.tigernet.backend.printing.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;

/**
 * Hàm định dạng dùng trong template, gọi qua biến {@code f}: ${f.money(x)}, ${f.day(d)}...
 * Mọi hàm nhận null và trả chuỗi rỗng để template tự in dòng chấm.
 */
public final class PrintFormatter {
    public static final PrintFormatter INSTANCE = new PrintFormatter();
    private static final DateTimeFormatter DMY = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private PrintFormatter() {
    }

    // 24000000 → "24.000.000" (kiểu Việt Nam, không phần lẻ)
    public String money(BigDecimal value) {
        if (value == null) {
            return "";
        }
        DecimalFormatSymbols symbols = new DecimalFormatSymbols();
        symbols.setGroupingSeparator('.');
        symbols.setDecimalSeparator(',');
        return new DecimalFormat("#,##0", symbols).format(value.setScale(0, RoundingMode.HALF_UP));
    }

    public String qty(Number value) {
        return value == null ? "" : money(new BigDecimal(value.toString()));
    }

    // số lượng 2 chữ số như trên biên bản bàn giao: 4 → "04"
    public String qty2(Number value) {
        if (value == null) {
            return "";
        }
        long v = value.longValue();
        return v >= 0 && v < 10 ? "0" + v : qty(value);
    }

    public String words(BigDecimal value) {
        return VnMoneyWords.of(value);
    }

    public String dmy(TemporalAccessor value) {
        return value == null ? "" : DMY.format(value);
    }

    public String day(TemporalAccessor value) {
        return part(value, "dd");
    }

    public String month(TemporalAccessor value) {
        return part(value, "MM");
    }

    public String year(TemporalAccessor value) {
        return part(value, "yyyy");
    }

    // "Ngày 05 tháng 01 năm 2022"
    public String longDate(TemporalAccessor value) {
        if (value == null) {
            return "Ngày …… tháng …… năm ……";
        }
        return "Ngày " + day(value) + " tháng " + month(value) + " năm " + year(value);
    }

    public LocalDate today() {
        return LocalDate.now();
    }

    public LocalDateTime now() {
        return LocalDateTime.now();
    }

    public boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private String part(TemporalAccessor value, String pattern) {
        return value == null ? "" : DateTimeFormatter.ofPattern(pattern).format(value);
    }
}
