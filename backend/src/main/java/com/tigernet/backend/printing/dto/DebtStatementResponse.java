package com.tigernet.backend.printing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** JSON xem trước đối chiếu công nợ trên màn hình (cùng số liệu với biên bản in). */
public record DebtStatementResponse(Integer partyId, String partyName, LocalDate from, LocalDate to,
                                    BigDecimal openingBalance, BigDecimal increase, BigDecimal decrease,
                                    BigDecimal closingBalance, List<Line> lines) {
    public record Line(LocalDate date, String code, String description, BigDecimal increase, BigDecimal decrease) {
    }

    public static DebtStatementResponse of(Integer partyId, String partyName, LocalDate from, LocalDate to,
                                           DebtStatement s) {
        return new DebtStatementResponse(partyId, partyName, from, to, s.opening(), s.increase(), s.decrease(),
                s.closing(), s.lines().stream()
                .map(l -> new Line(l.date(), l.voucher(), l.description(), l.increase(), l.decrease())).toList());
    }
}
