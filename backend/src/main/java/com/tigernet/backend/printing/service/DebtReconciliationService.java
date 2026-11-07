package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import com.tigernet.backend.partner.repository.CustomerRepository;
import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import com.tigernet.backend.printing.dto.DebtStatement;
import com.tigernet.backend.printing.repository.PartyPaymentRepository;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.enums.RentalStatus;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * Tính công nợ phải thu của một đối tác trong kỳ [from, to] (gồm cả hai đầu).
 * Tăng = phiếu xuất EXPORTED/CONFIRMED + tiền thuê (và phạt) của phiếu thuê APPROVED; giảm = Payments IN COMPLETED.
 * Phiếu hủy/chờ duyệt bị bỏ qua. Số dư đầu kỳ = tăng − giảm của mọi chứng từ trước {@code from}.
 */
@Service
@RequiredArgsConstructor
public class DebtReconciliationService {
    private final ExportReceiptRepository exportRepository;
    private final RentalReceiptRepository rentalRepository;
    private final PartyPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;

    public DebtStatement compute(Integer partyId, LocalDate from, LocalDate to) {
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<DebtStatement.Line> all = new ArrayList<>();

        customerRepository.findByPartyId(partyId).ifPresent(c -> {
            for (ExportReceiptEntity r : exportRepository.search(null, c.getId(), null, null, end)) {
                if (r.getStatus() == ExportStatus.EXPORTED || r.getStatus() == ExportStatus.CONFIRMED) {
                    all.add(new DebtStatement.Line(r.getDateOut().toLocalDate(), r.getReceiptCode(),
                            "Xuất bán hàng theo phiếu " + r.getReceiptCode(),
                            MoneyUtils.orZero(r.getTotalAmount()), BigDecimal.ZERO));
                }
            }
            for (RentalReceiptEntity r : rentalRepository.search(null, c.getId(), null, null, null, end)) {
                if (r.getStatus() == RentalStatus.APPROVED) {
                    all.add(new DebtStatement.Line(r.getDateOut().toLocalDate(), r.getReceiptCode(),
                            "Tiền thuê thiết bị theo phiếu " + r.getReceiptCode(),
                            MoneyUtils.orZero(r.getTotalRent()).add(MoneyUtils.orZero(r.getPenaltyAmount())),
                            BigDecimal.ZERO));
                }
            }
        });
        for (PaymentEntity p : paymentRepository
                .findByPartyIdAndDirectionAndStatusAndPaidAtBeforeOrderByPaidAtAscIdAsc(
                        partyId, PaymentDirection.IN, PaymentEntryStatus.COMPLETED, end)) {
            String note = p.getNote() != null && !p.getNote().isBlank() ? p.getNote() : "Thu tiền khách hàng";
            all.add(new DebtStatement.Line(p.getPaidAt().toLocalDate(), p.getPaymentCode(), note,
                    BigDecimal.ZERO, MoneyUtils.orZero(p.getAmount())));
        }
        return fold(all, from, to);
    }

    /** Phần thuần tính số: chia chứng từ thành trước kỳ (→ số dư đầu) và trong kỳ (→ phát sinh). */
    static DebtStatement fold(List<DebtStatement.Line> all, LocalDate from, LocalDate to) {
        BigDecimal opening = BigDecimal.ZERO;
        BigDecimal inc = BigDecimal.ZERO;
        BigDecimal dec = BigDecimal.ZERO;
        List<DebtStatement.Line> inPeriod = new ArrayList<>();
        for (DebtStatement.Line l : all) {
            if (l.date().isBefore(from)) {
                opening = opening.add(l.increase()).subtract(l.decrease());
            } else if (!l.date().isAfter(to)) {
                inc = inc.add(l.increase());
                dec = dec.add(l.decrease());
                inPeriod.add(l);
            }
        }
        inPeriod.sort(Comparator.comparing(DebtStatement.Line::date));
        return new DebtStatement(opening, inc, dec, opening.add(inc).subtract(dec), inPeriod);
    }
}
