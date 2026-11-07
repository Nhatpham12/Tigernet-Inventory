package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.printing.core.PdfRenderService;
import com.tigernet.backend.printing.core.PrintLine;
import com.tigernet.backend.printing.core.PrintParty;
import com.tigernet.backend.printing.core.PrintResponses;
import com.tigernet.backend.salesreturn.entity.SalesReturnDetailEntity;
import com.tigernet.backend.salesreturn.entity.SalesReturnEntity;
import com.tigernet.backend.salesreturn.repository.SalesReturnRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/** Phiếu nhận hàng trả lại / đổi hàng (mẫu nội bộ). */
@Service
@RequiredArgsConstructor
public class SalesReturnPrintService {
    private final SalesReturnRepository repository;
    private final PdfRenderService renderer;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> print(Integer id, String format, boolean download) {
        SalesReturnEntity r = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.SALES_RETURN_NOT_FOUND));
        ExportReceiptEntity original = r.getExportReceipt();

        List<PrintLine> lines = new ArrayList<>();
        int stt = 1;
        for (SalesReturnDetailEntity d : r.getDetails()) {
            BigDecimal price = MoneyUtils.orZero(d.getExportDetail().getPriceOut());
            lines.add(new PrintLine(stt++, d.getProduct().getProductName(), PrintLine.productCode(d.getProduct()),
                    d.getProduct().getUnit(), d.getSerial(), d.getQuantity(), d.getQuantity(), price,
                    price.multiply(BigDecimal.valueOf(d.getQuantity())), null, conditionText(d)));
        }
        BigDecimal refund = MoneyUtils.orZero(r.getRefundAmount());
        ExportReceiptEntity next = r.getNewExportReceipt();

        Map<String, Object> m = new HashMap<>();
        m.put("code", r.getReceiptCode());
        m.put("date", r.getReturnDate());
        m.put("customer", PrintParty.of(r.getParty()));
        m.put("originalCode", original.getReceiptCode());
        m.put("originalDate", original.getDeliveryDate() != null ? original.getDeliveryDate() : original.getDateOut());
        m.put("typeText", typeText(r));
        m.put("exchange", r.getReturnType() == com.tigernet.backend.salesreturn.enums.ReturnType.EXCHANGE);
        m.put("days", r.getDaysSinceDelivery());
        m.put("warehouseName", r.getWarehouse().getWarehouseName());
        m.put("storekeeper", r.getWarehouse().getManager() != null ? r.getWarehouse().getManager().getFullName() : null);
        m.put("createdBy", r.getCreatedBy() != null ? r.getCreatedBy().getFullName() : null);
        m.put("lines", lines);
        m.put("originalValue", MoneyUtils.orZero(r.getOriginalValue()));
        m.put("feePercent", r.getFeePercent() == null ? "0" : r.getFeePercent().stripTrailingZeros().toPlainString().replace('.', ','));
        m.put("feeAmount", MoneyUtils.orZero(r.getFeeAmount()));
        m.put("newCode", next != null ? next.getReceiptCode() : null);
        m.put("newValue", next != null ? MoneyUtils.orZero(next.getTotalAmount()) : null);
        m.put("payExtra", refund.signum() < 0);
        m.put("refundAbs", refund.abs());
        m.put("note", r.getNote());
        return PrintResponses.of(renderer, "phieu-tra-doi-hang", m, "Phieu_tra_doi_hang_" + r.getReceiptCode(), format, download);
    }

    private String typeText(SalesReturnEntity r) {
        return switch (r.getReturnType()) {
            case REFUND -> "Trả hàng";
            case EXCHANGE -> "Đổi hàng";
            case WARRANTY -> "Bảo hành";
        };
    }

    private String conditionText(SalesReturnDetailEntity d) {
        return switch (d.getReturnCondition()) {
            case RESELLABLE -> "Còn nguyên, bán lại được";
            case DAMAGED -> "Hư hỏng";
            case TO_WARRANTY -> "Chuyển bảo hành";
        };
    }
}
