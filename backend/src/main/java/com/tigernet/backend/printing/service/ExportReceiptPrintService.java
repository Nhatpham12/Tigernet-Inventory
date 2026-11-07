package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
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
import java.util.Locale;
import java.util.Map;

/**
 * Biểu mẫu in của phiếu xuất: invoice = Phiếu xuất kho 02-VT, delivery = Phiếu gửi hàng A5,
 * handover = Biên bản bàn giao | Phiếu bảo hành.
 */
@Service
@RequiredArgsConstructor
public class ExportReceiptPrintService {
    // bán hàng: Nợ 632 (giá vốn) / Có 1561 (hàng hóa)
    static final String DEBIT = "632";
    static final String CREDIT = "1561";
    static final int MAX_PARCELS = 99;

    private final ExportReceiptRepository repository;
    private final PdfRenderService renderer;

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> print(Integer id, String template, Integer parcels, String format, boolean download) {
        String t = template == null || template.isBlank() ? "invoice" : template.trim().toLowerCase(Locale.ROOT);
        if (!t.equals("invoice") && !t.equals("delivery") && !t.equals("handover")) {
            throw new AppException(ErrorCode.PRINT_TEMPLATE_INVALID);
        }
        ExportReceiptEntity r = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.EXPORT_RECEIPT_NOT_FOUND));
        PrintParty customer = PrintParty.of(r.getCustomer() == null ? null : r.getCustomer().getParty());
        PrintParty receiver = receiver(r, customer);

        return switch (t) {
            case "delivery" -> PrintResponses.of(renderer, "phieu_gui_hang", delivery(r, receiver, parcels),
                    "Phieu_gui_hang_" + r.getReceiptCode(), format, download);
            case "handover" -> PrintResponses.of(renderer, "bien_ban_ban_giao_bao_hanh", handover(r, customer, receiver),
                    "Bien_ban_ban_giao_" + r.getReceiptCode(), format, download);
            default -> PrintResponses.of(renderer, "02-VT_phieu_xuat_kho", invoice(r, customer, receiver),
                    "Phieu_xuat_kho_" + r.getReceiptCode(), format, download);
        };
    }

    // người nhận hàng: snapshot trên phiếu, trường nào rỗng thì lấy thông tin khách
    static PrintParty receiver(ExportReceiptEntity r, PrintParty customer) {
        return new PrintParty(
                firstNonBlank(r.getReceiverNameSnapshot(), customer.name()),
                null, null,
                firstNonBlank(r.getReceiverAddressSnapshot(), customer.address()),
                firstNonBlank(r.getReceiverPhoneSnapshot(), customer.phone()),
                null);
    }

    static String firstNonBlank(String a, String b) {
        return a != null && !a.isBlank() ? a : b;
    }

    private static List<PrintLine> lines(ExportReceiptEntity r) {
        List<PrintLine> lines = new ArrayList<>();
        int stt = 1;
        for (ExportReceiptDetailEntity d : r.getDetails()) {
            lines.add(new PrintLine(stt++, d.getProduct().getProductName(), PrintLine.productCode(d.getProduct()),
                    d.getProduct().getUnit(), d.getSerial(), d.getQuantity(), d.getQuantity(),
                    d.getPriceOut(), MoneyUtils.orZero(d.getLineTotal()), d.getWarrantyMonths(), null));
        }
        return lines;
    }

    private Map<String, Object> invoice(ExportReceiptEntity r, PrintParty customer, PrintParty receiver) {
        List<PrintLine> lines = lines(r);
        BigDecimal subtotal = lines.stream().map(PrintLine::amount).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal total = MoneyUtils.orZero(r.getTotalAmount());
        BigDecimal vat = total.subtract(subtotal);

        Map<String, Object> m = new HashMap<>();
        m.put("code", r.getReceiptCode());
        m.put("date", r.getDateOut());
        m.put("debit", DEBIT);
        m.put("credit", CREDIT);
        m.put("receiver", receiver);
        m.put("reason", "Xuất bán hàng" + (customer.name() == null ? "" : " – " + customer.name()));
        m.put("warehouseName", r.getWarehouse().getWarehouseName());
        m.put("warehouseAddress", r.getWarehouse().getAddress());
        m.put("lines", lines);
        m.put("subtotal", subtotal);
        m.put("vatAmount", vat.signum() > 0 ? vat : null);
        m.put("vatLabel", r.getVat() != null ? r.getVat().getVat() : null);
        m.put("total", total);
        m.put("note", r.getNote());
        return m;
    }

    private Map<String, Object> delivery(ExportReceiptEntity r, PrintParty receiver, Integer parcels) {
        List<String> pages = new ArrayList<>();
        if (parcels != null && parcels >= 1) {
            int n = Math.min(parcels, MAX_PARCELS);
            for (int i = 1; i <= n; i++) {
                pages.add(String.format("%02d/%02d", i, n));
            }
        } else {
            pages.add("…../…..");
        }
        Map<String, Object> m = new HashMap<>();
        m.put("code", r.getReceiptCode());
        m.put("receiver", receiver);
        m.put("greet", receiver.name() != null && receiver.name().matches("(?iu)^(ông|bà|anh|chị|mr|ms|mrs)(\\s.*)?") ? "" : "Ông ");
        m.put("lines", lines(r));
        m.put("pages", pages);
        return m;
    }

    private Map<String, Object> handover(ExportReceiptEntity r, PrintParty customer, PrintParty receiver) {
        Map<String, Object> m = new HashMap<>();
        m.put("code", "BBBG-PBH-" + r.getReceiptCode());
        m.put("date", r.getDeliveryDate() != null ? r.getDeliveryDate() : r.getDateOut());
        m.put("customer", customer);
        m.put("receiver", receiver);
        m.put("lines", lines(r));
        m.put("note", r.getNote());
        return m;
    }
}
