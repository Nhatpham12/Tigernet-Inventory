package com.tigernet.backend.printing.service;

import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.repository.CustomerRepository;
import com.tigernet.backend.partner.repository.SupplierRepository;
import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import com.tigernet.backend.printing.dto.DebtStatement;
import com.tigernet.backend.printing.repository.PartyPaymentRepository;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptEntity;
import com.tigernet.backend.rental.enums.RentalStatus;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
import com.tigernet.backend.rental.repository.ReturnReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;

/**
 * Tính công nợ của một đối tác trong kỳ [from, to] (gồm cả hai đầu). Số dư đầu kỳ = tăng − giảm của mọi chứng từ
 * trước {@code from}. Phiếu hủy/chờ duyệt bị bỏ qua.
 *
 * Mọi chứng từ ghi theo hướng "đối tác nợ công ty":
 * - tăng: phiếu xuất CONFIRMED/EXPORTED; phiếu thuê đã duyệt chưa hủy (tiền thuê + phạt); bồi thường trên phiếu trả;
 *   phiếu chi cho đối tác (OUT, VD trả tiền NCC, hoàn cọc cho khách).
 * - giảm: phiếu thu từ đối tác (IN); phiếu nhập CONFIRMED (công ty nợ NCC tiền hàng).
 * Đối tác chỉ là NCC (không là khách hàng) thì đảo chiều để biên bản đọc theo "công ty nợ NCC" ({@code payable}).
 */
@Service
@RequiredArgsConstructor
public class DebtReconciliationService {
    private final ExportReceiptRepository exportRepository;
    private final RentalReceiptRepository rentalRepository;
    private final PartyPaymentRepository paymentRepository;
    private final CustomerRepository customerRepository;
    private final ImportReceiptRepository importRepository;
    private final SupplierRepository supplierRepository;
    private final ReturnReceiptRepository returnRepository;

    public DebtStatement compute(Integer partyId, LocalDate from, LocalDate to) {
        LocalDateTime end = to.plusDays(1).atStartOfDay();
        List<DebtStatement.Line> all = new ArrayList<>();

        Optional<CustomerEntity> customer = customerRepository.findByPartyId(partyId);
        customer.ifPresent(c -> addCustomerDocuments(all, c, end));
        for (ImportReceiptEntity r : importRepository.search(null, ImportStatus.CONFIRMED, partyId, null, end)) {
            all.add(new DebtStatement.Line(r.getDateIn().toLocalDate(), r.getReceiptCode(),
                    "Mua hàng theo phiếu nhập " + r.getReceiptCode(),
                    BigDecimal.ZERO, MoneyUtils.orZero(r.getTotalAmount())));
        }
        for (PaymentEntity p : payments(partyId, PaymentDirection.IN, end)) {
            all.add(new DebtStatement.Line(p.getPaidAt().toLocalDate(), p.getPaymentCode(),
                    noteOr(p, "Thu tiền khách hàng"), BigDecimal.ZERO, MoneyUtils.orZero(p.getAmount())));
        }
        for (PaymentEntity p : payments(partyId, PaymentDirection.OUT, end)) {
            all.add(new DebtStatement.Line(p.getPaidAt().toLocalDate(), p.getPaymentCode(),
                    noteOr(p, "Chi tiền cho đối tác"), MoneyUtils.orZero(p.getAmount()), BigDecimal.ZERO));
        }

        boolean payable = customer.isEmpty() && supplierRepository.findByPartyId(partyId).isPresent();
        List<DebtStatement.Line> lines = !payable ? all : all.stream()
                .map(l -> new DebtStatement.Line(l.date(), l.voucher(), l.description(), l.decrease(), l.increase()))
                .toList();
        return fold(lines, from, to, payable);
    }

    // phiếu xuất đã duyệt, phiếu thuê chưa hủy (tiền thuê + phạt) và bồi thường trên các phiếu trả của khách
    private void addCustomerDocuments(List<DebtStatement.Line> all, CustomerEntity c, LocalDateTime end) {
        for (ExportReceiptEntity r : exportRepository.search(null, c.getId(), null, null, end)) {
            if (r.getStatus() == ExportStatus.EXPORTED || r.getStatus() == ExportStatus.CONFIRMED) {
                all.add(new DebtStatement.Line(r.getDateOut().toLocalDate(), r.getReceiptCode(),
                        "Xuất bán hàng theo phiếu " + r.getReceiptCode(),
                        MoneyUtils.orZero(r.getTotalAmount()), BigDecimal.ZERO));
            }
        }
        for (RentalReceiptEntity r : rentalRepository.search(null, c.getId(), null, null, null, end)) {
            boolean cancelled = r.getPaymentStatus() != null
                    && PaymentStatusEntity.CANCELLED.equals(r.getPaymentStatus().getPaymentStatus());
            if (r.getStatus() != RentalStatus.APPROVED || cancelled) {
                continue;
            }
            all.add(new DebtStatement.Line(r.getDateOut().toLocalDate(), r.getReceiptCode(),
                    "Tiền thuê thiết bị theo phiếu " + r.getReceiptCode(),
                    MoneyUtils.orZero(r.getTotalRent()).add(MoneyUtils.orZero(r.getPenaltyAmount())),
                    BigDecimal.ZERO));
            for (ReturnReceiptEntity back : returnRepository.search(null, r.getId())) {
                BigDecimal compensation = MoneyUtils.orZero(back.getCompensationAmount());
                if (compensation.signum() > 0 && back.getReturnDate().isBefore(end)) {
                    all.add(new DebtStatement.Line(back.getReturnDate().toLocalDate(), back.getReceiptCode(),
                            "Bồi thường hư hỏng/mất theo phiếu trả " + back.getReceiptCode(),
                            compensation, BigDecimal.ZERO));
                }
            }
        }
    }

    private List<PaymentEntity> payments(Integer partyId, PaymentDirection direction, LocalDateTime end) {
        return paymentRepository.findByPartyIdAndDirectionAndStatusAndPaidAtBeforeOrderByPaidAtAscIdAsc(
                partyId, direction, PaymentEntryStatus.COMPLETED, end);
    }

    private static String noteOr(PaymentEntity p, String fallback) {
        return p.getNote() != null && !p.getNote().isBlank() ? p.getNote() : fallback;
    }

    static DebtStatement fold(List<DebtStatement.Line> all, LocalDate from, LocalDate to) {
        return fold(all, from, to, false);
    }

    /** Phần thuần tính số: chia chứng từ thành trước kỳ (→ số dư đầu) và trong kỳ (→ phát sinh). */
    static DebtStatement fold(List<DebtStatement.Line> all, LocalDate from, LocalDate to, boolean payable) {
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
        return new DebtStatement(opening, inc, dec, opening.add(inc).subtract(dec), inPeriod, payable);
    }
}
