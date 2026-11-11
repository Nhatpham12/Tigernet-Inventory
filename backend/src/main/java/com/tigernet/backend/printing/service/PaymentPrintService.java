package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentMethod;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import com.tigernet.backend.payment.repository.PaymentRepository;
import com.tigernet.backend.printing.core.PdfRenderService;
import com.tigernet.backend.printing.core.PrintParty;
import com.tigernet.backend.printing.core.PrintResponses;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/** Phiếu thu mẫu 01-TT (Direction IN) và phiếu chi mẫu 02-TT (Direction OUT), TT200. */
@Service
@RequiredArgsConstructor
public class PaymentPrintService {
    static final String CASH_ACCOUNT = "1111";     // tiền mặt
    static final String BANK_ACCOUNT = "1121";     // tiền gửi ngân hàng
    static final String RECEIVABLE_ACCOUNT = "131"; // phải thu khách hàng
    static final String PAYABLE_ACCOUNT = "331";    // phải trả người bán

    private final PaymentRepository repository;
    private final ImportReceiptRepository importRepository;
    private final ExportReceiptRepository exportRepository;
    private final RentalReceiptRepository rentalRepository;
    private final PdfRenderService renderer;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> print(Integer id, String format, boolean download) {
        PaymentEntity p = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.PAYMENT_NOT_FOUND));
        boolean in = p.getDirection() == PaymentDirection.IN;
        String[] accounts = accounts(p);

        Map<String, Object> m = new HashMap<>();
        m.put("receipt", in);
        m.put("formNo", in ? "01-TT" : "02-TT");
        m.put("title", in ? "PHIẾU THU" : "PHIẾU CHI");
        m.put("code", p.getPaymentCode());
        m.put("date", p.getPaidAt());
        m.put("debit", accounts[0]);
        m.put("credit", accounts[1]);
        m.put("party", PrintParty.of(p.getParty()));
        m.put("reason", reason(p, in));
        m.put("amount", MoneyUtils.orZero(p.getAmount()));
        m.put("createdBy", p.getRecordedBy() != null ? p.getRecordedBy().getFullName() : null);
        return PrintResponses.of(renderer, "TT_phieu_thu_chi", m,
                PrintResponses.fileName(in ? "Phieu_thu" : "Phieu_chi", p.getPaidAt()), format, download);
    }

    /** Nợ/Có mặc định: thu tiền bán hàng/cho thuê Nợ 1111 Có 131; chi trả NCC Nợ 331 Có 1111; chuyển khoản thay 1111 bằng 1121. */
    static String[] accounts(PaymentEntity p) {
        String money = p.getMethod() == PaymentMethod.TRANSFER ? BANK_ACCOUNT : CASH_ACCOUNT;
        PaymentReferenceType ref = p.getReferenceType();
        if (p.getDirection() == PaymentDirection.IN
                && (ref == PaymentReferenceType.Export_Receipts || ref == PaymentReferenceType.Rental_Receipts)) {
            return new String[]{money, RECEIVABLE_ACCOUNT};
        }
        if (p.getDirection() == PaymentDirection.OUT && ref == PaymentReferenceType.Import_Receipts) {
            return new String[]{PAYABLE_ACCOUNT, money};
        }
        return new String[]{null, null};
    }

    private String reason(PaymentEntity p, boolean in) {
        if (p.getNote() != null && !p.getNote().isBlank()) {
            return p.getNote();
        }
        String code = referenceCode(p);
        String label = switch (p.getReferenceType()) {
            case Import_Receipts -> "phiếu nhập kho";
            case Export_Receipts -> "phiếu xuất kho";
            case Rental_Receipts -> "phiếu cho thuê";
            case Return_Receipts -> "phiếu trả hàng thuê";
            case Sales_Returns -> "phiếu khách trả hàng";
            case Supplier_Returns -> "phiếu trả hàng NCC";
        };
        return (in ? "Thu tiền theo " : "Chi tiền theo ") + label + (code != null ? " " + code : "");
    }

    private String referenceCode(PaymentEntity p) {
        Integer rid = p.getReferenceId();
        return switch (p.getReferenceType()) {
            case Import_Receipts -> importRepository.findById(rid).map(r -> r.getReceiptCode()).orElse(null);
            case Export_Receipts -> exportRepository.findById(rid).map(r -> r.getReceiptCode()).orElse(null);
            case Rental_Receipts -> rentalRepository.findById(rid).map(r -> r.getReceiptCode()).orElse(null);
            default -> null;
        };
    }
}
