package com.tigernet.backend.outbound.service;

import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.service.LookupService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

// Paid/Remain/Payment_Status khi cộng hoặc trừ một lần thu (dùng cho phiếu thu và hủy phiếu thu)
@ExtendWith(MockitoExtension.class)
class ExportReceiptAdjustPaidTest {
    @Mock ExportReceiptRepository exportReceiptRepository;
    @Mock LookupService lookupService;
    @InjectMocks ExportReceiptService service;

    @BeforeEach
    void setUp() {
        lenient().when(exportReceiptRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        lenient().when(lookupService.findPaymentStatus(anyString())).thenAnswer(i -> {
            PaymentStatusEntity s = new PaymentStatusEntity();
            s.setPaymentStatus(i.getArgument(0));
            return s;
        });
    }

    private static ExportReceiptEntity receipt(long total, long paid) {
        ExportReceiptDetailEntity d = new ExportReceiptDetailEntity();
        d.setLineTotal(BigDecimal.valueOf(total));
        ExportReceiptEntity r = new ExportReceiptEntity();
        r.setDetails(List.of(d));
        r.setPaid(BigDecimal.valueOf(paid));
        return r;
    }

    @Test
    void partialThenFullThenCancelBack() {
        ExportReceiptEntity r = receipt(1_000_000, 0);
        service.adjustPaid(r, BigDecimal.valueOf(400_000));
        assertEquals(0, BigDecimal.valueOf(600_000).compareTo(r.getRemain()));
        assertEquals(PaymentStatusEntity.PARTIAL, r.getPaymentStatus().getPaymentStatus());

        service.adjustPaid(r, BigDecimal.valueOf(600_000));
        assertEquals(0, r.getRemain().signum());
        assertEquals(PaymentStatusEntity.PAID, r.getPaymentStatus().getPaymentStatus());

        service.adjustPaid(r, BigDecimal.valueOf(-1_000_000));
        assertEquals(0, BigDecimal.valueOf(1_000_000).compareTo(r.getRemain()));
        assertEquals(PaymentStatusEntity.UNPAID, r.getPaymentStatus().getPaymentStatus());
    }

    @Test
    void overpaymentIsRejected() {
        ExportReceiptEntity r = receipt(1_000_000, 900_000);
        AppException e = assertThrows(AppException.class, () -> service.adjustPaid(r, BigDecimal.valueOf(100_001)));
        assertEquals(ErrorCode.PAID_EXCEEDS_TOTAL, e.getErrorCode());
    }
}
