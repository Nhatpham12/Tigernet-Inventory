package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inbound.entity.ImportReceiptDetailEntity;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.printing.core.PdfRenderService;
import com.tigernet.backend.printing.core.PrintLine;
import com.tigernet.backend.printing.core.PrintParty;
import com.tigernet.backend.printing.core.PrintResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Phiếu nhập kho – mẫu 01-VT (TT200). */
@Service
@RequiredArgsConstructor
public class ImportReceiptPrintService {
    // tài khoản mặc định khi mua hàng chưa trả tiền: Nợ 1561 (giá mua hàng hóa) / Có 331 (phải trả NCC)
    static final String DEBIT = "1561";
    static final String CREDIT = "331";

    private final ImportReceiptRepository repository;
    private final PdfRenderService renderer;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> print(Integer id, String format, boolean download) {
        ImportReceiptEntity r = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.IMPORT_RECEIPT_NOT_FOUND));

        List<PrintLine> lines = new ArrayList<>();
        BigDecimal subtotal = BigDecimal.ZERO;
        int stt = 1;
        for (ImportReceiptDetailEntity d : r.getDetails()) {
            BigDecimal amount = MoneyUtils.orZero(d.getLineTotal());
            subtotal = subtotal.add(amount);
            lines.add(new PrintLine(stt++, d.getProduct().getProductName(), PrintLine.productCode(d.getProduct()),
                    d.getProduct().getUnit(), d.getSerial(), d.getQuantity(), d.getQuantity(),
                    d.getPriceIn(), amount, null, null));
        }
        BigDecimal total = MoneyUtils.orZero(r.getTotalAmount());
        BigDecimal vat = total.subtract(subtotal);

        Map<String, Object> m = new HashMap<>();
        m.put("code", r.getReceiptCode());
        m.put("date", r.getDateIn());
        m.put("debit", DEBIT);
        m.put("credit", CREDIT);
        m.put("supplier", PrintParty.of(r.getSourceParty()));
        m.put("warehouseName", r.getWarehouse().getWarehouseName());
        m.put("warehouseAddress", r.getWarehouse().getAddress());
        m.put("storekeeper", r.getWarehouse().getManager() != null ? r.getWarehouse().getManager().getFullName() : null);
        m.put("createdBy", r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null);
        m.put("lines", lines);
        m.put("subtotal", subtotal);
        m.put("vatAmount", vat.signum() > 0 ? vat : null);
        m.put("vatLabel", r.getVat() != null ? r.getVat().getVat() : null);
        m.put("total", total);
        m.put("note", r.getNote());
        return PrintResponses.of(renderer, "01-VT_phieu_nhap_kho", m, PrintResponses.fileName("Phieu_nhap_kho", r.getDateIn()), format, download);
    }
}
