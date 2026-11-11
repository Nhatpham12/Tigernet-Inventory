package com.tigernet.backend.printing.service;

import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.repository.CustomerRepository;
import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import com.tigernet.backend.printing.dto.DebtStatement;
import com.tigernet.backend.printing.repository.PartyPaymentRepository;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.enums.RentalStatus;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DebtReconciliationServiceTest {
    @Mock ExportReceiptRepository exportRepository;
    @Mock RentalReceiptRepository rentalRepository;
    @Mock PartyPaymentRepository paymentRepository;
    @Mock CustomerRepository customerRepository;

    DebtReconciliationService service;

    @BeforeEach
    void setUp() {
        service = new DebtReconciliationService(exportRepository, rentalRepository, paymentRepository, customerRepository);
    }

    private static BigDecimal n(long v) {
        return BigDecimal.valueOf(v);
    }

    private static ExportReceiptEntity export(String code, String date, long total, ExportStatus st) {
        ExportReceiptEntity e = new ExportReceiptEntity();
        e.setReceiptCode(code);
        e.setDateOut(LocalDateTime.parse(date + "T09:00:00"));
        e.setTotalAmount(n(total));
        e.setStatus(st);
        return e;
    }

    private static PaymentEntity pay(String code, String date, long amount) {
        PaymentEntity p = new PaymentEntity();
        p.setPaymentCode(code);
        p.setPaidAt(LocalDateTime.parse(date + "T10:00:00"));
        p.setAmount(n(amount));
        return p;
    }

    @Test
    void tinhSoDuDauKyPhatSinhVaCuoiKy() {
        CustomerEntity c = new CustomerEntity();
        c.setId(5);
        when(customerRepository.findByPartyId(1)).thenReturn(Optional.of(c));
        when(exportRepository.search(eq(null), eq(5), eq(null), eq(null), any())).thenReturn(List.of(
                export("PX1", "2026-09-15", 40_000_000, ExportStatus.EXPORTED),
                export("PX2", "2026-10-12", 12_000_000, ExportStatus.CONFIRMED),
                export("PX3", "2026-10-20", 5_000_000, ExportStatus.CANCELLED),
                export("PX4", "2026-10-21", 7_000_000, ExportStatus.PENDING)));
        RentalReceiptEntity r = new RentalReceiptEntity();
        r.setReceiptCode("PT1");
        r.setDateOut(LocalDateTime.parse("2026-10-25T09:00:00"));
        r.setTotalRent(n(3_000_000));
        r.setPenaltyAmount(n(500_000));
        r.setStatus(RentalStatus.APPROVED);
        RentalReceiptEntity rejected = new RentalReceiptEntity();
        rejected.setReceiptCode("PT2");
        rejected.setDateOut(LocalDateTime.parse("2026-10-26T09:00:00"));
        rejected.setTotalRent(n(9_000_000));
        rejected.setStatus(RentalStatus.REJECTED);
        when(rentalRepository.search(eq(null), eq(5), eq(null), eq(null), eq(null), any())).thenReturn(List.of(r, rejected));
        when(paymentRepository.findByPartyIdAndDirectionAndStatusAndPaidAtBeforeOrderByPaidAtAscIdAsc(
                eq(1), eq(PaymentDirection.IN), eq(PaymentEntryStatus.COMPLETED), any())).thenReturn(List.of(
                pay("T1", "2026-09-30", 15_000_000),
                pay("T2", "2026-10-28", 20_000_000),
                pay("T3", "2026-10-30", 1_000_000)));

        DebtStatement s = service.compute(1, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(0, n(25_000_000).compareTo(s.opening()));
        assertEquals(0, n(15_500_000).compareTo(s.increase()));
        assertEquals(0, n(21_000_000).compareTo(s.decrease()));
        assertEquals(0, n(19_500_000).compareTo(s.closing()));
        assertEquals(4, s.lines().size());
        assertEquals("PX2", s.lines().get(0).voucher());
    }

    @Test
    void khongPhaiKhachHangThiChiTinhThanhToan() {
        when(customerRepository.findByPartyId(2)).thenReturn(Optional.empty());
        when(paymentRepository.findByPartyIdAndDirectionAndStatusAndPaidAtBeforeOrderByPaidAtAscIdAsc(
                eq(2), eq(PaymentDirection.IN), eq(PaymentEntryStatus.COMPLETED), any()))
                .thenReturn(List.of(pay("T1", "2026-10-02", 2_000_000)));

        DebtStatement s = service.compute(2, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));

        assertEquals(0, n(-2_000_000).compareTo(s.closing()));
        assertEquals(0, BigDecimal.ZERO.compareTo(s.opening()));
    }

    @Test
    void ngayBienLaTinhVaoKy() {
        List<DebtStatement.Line> lines = List.of(
                new DebtStatement.Line(LocalDate.of(2026, 9, 30), "A", "", n(100), BigDecimal.ZERO),
                new DebtStatement.Line(LocalDate.of(2026, 10, 1), "B", "", n(10), BigDecimal.ZERO),
                new DebtStatement.Line(LocalDate.of(2026, 10, 31), "C", "", BigDecimal.ZERO, n(4)),
                new DebtStatement.Line(LocalDate.of(2026, 11, 1), "D", "", n(1000), BigDecimal.ZERO));
        DebtStatement s = DebtReconciliationService.fold(lines, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 31));
        assertEquals(0, n(100).compareTo(s.opening()));
        assertEquals(0, n(10).compareTo(s.increase()));
        assertEquals(0, n(4).compareTo(s.decrease()));
        assertEquals(0, n(106).compareTo(s.closing()));
        assertEquals(2, s.lines().size());
    }

    @Test
    void khongCoChungTuThiTatCaBangKhong() {
        DebtStatement s = DebtReconciliationService.fold(List.of(), LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        assertTrue(s.lines().isEmpty());
        assertEquals(0, BigDecimal.ZERO.compareTo(s.closing()));
    }
}
