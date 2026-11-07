package com.tigernet.backend.printing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** Kết quả đối chiếu công nợ một đối tác trong kỳ. */
public record DebtStatement(BigDecimal opening, BigDecimal increase, BigDecimal decrease, BigDecimal closing,
                            List<Line> lines) {
    /** Một chứng từ trong kỳ. */
    public record Line(LocalDate date, String voucher, String description, BigDecimal increase, BigDecimal decrease) {
    }
}
