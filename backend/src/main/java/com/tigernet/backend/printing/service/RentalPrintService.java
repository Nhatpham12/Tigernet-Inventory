package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.printing.core.PdfRenderService;
import com.tigernet.backend.printing.core.PrintLine;
import com.tigernet.backend.printing.core.PrintParty;
import com.tigernet.backend.printing.core.PrintResponses;
import com.tigernet.backend.rental.entity.RentalReceiptDetailEntity;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptDetailEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptEntity;
import com.tigernet.backend.rental.enums.RentalType;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
import com.tigernet.backend.rental.repository.ReturnReceiptRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Biên bản bàn giao cho thuê / cho mượn và biên bản trả hàng thuê / mượn. */
@Service
@RequiredArgsConstructor
public class RentalPrintService {
    private final RentalReceiptRepository rentalRepository;
    private final ReturnReceiptRepository returnRepository;
    private final PdfRenderService renderer;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> printRental(Integer id, String format, boolean download) {
        RentalReceiptEntity r = rentalRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RENTAL_RECEIPT_NOT_FOUND));
        boolean lend = r.getRentalType() == RentalType.LEND;

        List<PrintLine> lines = new ArrayList<>();
        int stt = 1;
        for (RentalReceiptDetailEntity d : r.getDetails()) {
            lines.add(new PrintLine(stt++, d.getProduct().getProductName(), PrintLine.productCode(d.getProduct()),
                    d.getProduct().getUnit(), d.getSerial(), d.getQuantity(), d.getQuantity(),
                    d.getRentalPrice(), null, null, null));
        }

        Map<String, Object> m = new HashMap<>();
        m.put("lend", lend);
        m.put("code", r.getReceiptCode());
        m.put("docNo", (lend ? "BBBG-CM-" : "BBBG-CT-") + stripPrefix(r.getReceiptCode(), lend ? "CM-" : "CT-"));
        m.put("customer", PrintParty.of(r.getCustomer().getParty()));
        m.put("receiverName", r.getReceiverNameSnapshot() != null ? r.getReceiverNameSnapshot()
                : r.getReceiver() != null ? r.getReceiver().getReceiverName() : null);
        m.put("receiverPhone", r.getReceiverPhoneSnapshot() != null ? r.getReceiverPhoneSnapshot()
                : r.getReceiver() != null ? r.getReceiver().getPhone() : null);
        m.put("receiverAddress", r.getReceiverAddressSnapshot() != null ? r.getReceiverAddressSnapshot()
                : r.getReceiver() != null ? r.getReceiver().getAddress() : null);
        m.put("dateOut", r.getDateOut());
        m.put("dueDate", r.getDueDate());
        m.put("pricePerDay", MoneyUtils.orZero(r.getRentalPricePerDay()));
        m.put("deposit", MoneyUtils.orZero(r.getDeposit()));
        m.put("lines", lines);
        return PrintResponses.of(renderer, "bien_ban_ban_giao_thue", m,
                "Bien_ban_ban_giao_" + (lend ? "cho_muon_" : "cho_thue_") + r.getReceiptCode(), format, download);
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> printReturn(Integer id, String format, boolean download) {
        ReturnReceiptEntity t = returnRepository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.RETURN_RECEIPT_NOT_FOUND));
        RentalReceiptEntity r = t.getRentalReceipt();
        boolean lend = r.getRentalType() == RentalType.LEND;

        List<PrintLine> lines = new ArrayList<>();
        int stt = 1;
        for (ReturnReceiptDetailEntity d : t.getDetails()) {
            lines.add(new PrintLine(stt++, d.getProduct().getProductName(), PrintLine.productCode(d.getProduct()),
                    d.getProduct().getUnit(), d.getSerial(), d.getQuantity(), d.getQuantity(),
                    null, null, null, d.getConditionNote()));
        }

        Map<String, Object> m = new HashMap<>();
        m.put("lend", lend);
        m.put("code", t.getReceiptCode());
        m.put("docNo", "BBTH-" + stripPrefix(t.getReceiptCode(), "TH-"));
        m.put("rentalCode", r.getReceiptCode());
        m.put("rentalDate", r.getDateOut());
        m.put("dueDate", r.getDueDate());
        m.put("customer", PrintParty.of(r.getCustomer().getParty()));
        m.put("returnerName", r.getReceiverNameSnapshot());
        m.put("returnerPhone", r.getReceiverPhoneSnapshot());
        m.put("returnDate", t.getReturnDate());
        m.put("warehouseName", t.getWarehouse() != null ? t.getWarehouse().getWarehouseName() : null);
        m.put("warehouseAddress", t.getWarehouse() != null ? t.getWarehouse().getAddress() : null);
        m.put("daysOverdue", t.getDaysOverdue() == null ? 0 : t.getDaysOverdue());
        m.put("penalty", MoneyUtils.orZero(t.getPenaltyAmount()));
        m.put("compensation", MoneyUtils.orZero(t.getCompensationAmount()));
        m.put("deposit", MoneyUtils.orZero(r.getDeposit()));
        m.put("depositRefund", MoneyUtils.orZero(t.getDepositRefund()));
        m.put("note", t.getNote());
        m.put("lines", lines);
        return PrintResponses.of(renderer, "bien_ban_tra_hang_thue", m,
                "Bien_ban_tra_hang_" + t.getReceiptCode(), format, download);
    }

    // mã phiếu đã có tiền tố loại phiếu (CT-/CM-/TH-) thì bỏ đi để số biên bản không bị lặp
    private static String stripPrefix(String code, String prefix) {
        return code != null && code.startsWith(prefix) ? code.substring(prefix.length()) : code;
    }
}
