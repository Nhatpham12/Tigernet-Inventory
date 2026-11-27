package com.tigernet.backend.printing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Kết quả đối chiếu công nợ một đối tác trong kỳ.
 * payable = false: số dư dương là đối tác nợ công ty (khách hàng); true: số dư dương là công ty nợ đối tác (NCC).
 */
public record DebtStatement(BigDecimal opening, BigDecimal increase, BigDecimal decrease, BigDecimal closing,
                            List<Line> lines, boolean payable) {
    /** Một chứng từ trong kỳ. */
    public record Line(LocalDate date, String voucher, String description, BigDecimal increase, BigDecimal decrease) {
    }
}
