package com.tigernet.backend.printing.core;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Đọc số tiền thành chữ tiếng Việt cho dòng "Bằng chữ" trên phiếu,
 * VD 52500000 → "Năm mươi hai triệu năm trăm nghìn đồng chẵn".
 */
public final class VnMoneyWords {
    private static final String[] DIGITS = {"không", "một", "hai", "ba", "bốn", "năm", "sáu", "bảy", "tám", "chín"};
    private static final String[] UNITS = {"", " nghìn", " triệu", " tỷ"};

    private VnMoneyWords() {
    }

    public static String of(BigDecimal amount) {
        if (amount == null) {
            return "";
        }
        long value = amount.setScale(0, RoundingMode.HALF_UP).longValueExact();
        boolean negative = value < 0;
        value = Math.abs(value);
        String words = value == 0 ? "không" : readNumber(value);
        String sentence = (negative ? "âm " : "") + words + " đồng chẵn";
        return Character.toUpperCase(sentence.charAt(0)) + sentence.substring(1);
    }

    // đọc số nguyên dương theo nhóm 3 chữ số; nhóm "tỷ" lặp lại cho số rất lớn
    private static String readNumber(long value) {
        if (value >= 1_000_000_000L) {
            long billions = value / 1_000_000_000L;
            long rest = value % 1_000_000_000L;
            String head = readNumber(billions) + " tỷ";
            return rest == 0 ? head : head + " " + readBelowBillion(rest, true);
        }
        return readBelowBillion(value, false);
    }

    private static String readBelowBillion(long value, boolean hasHigher) {
        int[] groups = {(int) (value % 1000), (int) (value / 1000 % 1000), (int) (value / 1_000_000 % 1000)};
        StringBuilder sb = new StringBuilder();
        boolean started = hasHigher;
        for (int i = 2; i >= 0; i--) {
            int g = groups[i];
            if (g == 0) {
                continue;
            }
            if (!sb.isEmpty()) {
                sb.append(' ');
            }
            sb.append(readGroup(g, started)).append(UNITS[i]);
            started = true;
        }
        return sb.toString();
    }

    // đọc nhóm 3 chữ số; full = có nhóm lớn hơn đứng trước nên phải đọc đủ "không trăm", "lẻ"
    private static String readGroup(int g, boolean full) {
        int hundreds = g / 100;
        int tens = g / 10 % 10;
        int ones = g % 10;
        StringBuilder sb = new StringBuilder();
        if (hundreds > 0 || full) {
            sb.append(DIGITS[hundreds]).append(" trăm");
        }
        if (tens > 1) {
            sb.append(sb.isEmpty() ? "" : " ").append(DIGITS[tens]).append(" mươi");
            if (ones == 1) {
                sb.append(" mốt");
            } else if (ones == 4) {
                sb.append(" tư");
            } else if (ones == 5) {
                sb.append(" lăm");
            } else if (ones > 0) {
                sb.append(' ').append(DIGITS[ones]);
            }
        } else if (tens == 1) {
            sb.append(sb.isEmpty() ? "" : " ").append("mười");
            if (ones == 5) {
                sb.append(" lăm");
            } else if (ones > 0) {
                sb.append(' ').append(DIGITS[ones]);
            }
        } else if (ones > 0) {
            if (!sb.isEmpty()) {
                sb.append(" lẻ");
            }
            sb.append(sb.isEmpty() ? "" : " ").append(DIGITS[ones]);
        }
        return sb.toString();
    }
}
