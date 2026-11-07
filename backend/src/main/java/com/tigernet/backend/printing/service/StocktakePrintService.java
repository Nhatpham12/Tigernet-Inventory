package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.printing.core.PdfRenderService;
import com.tigernet.backend.printing.core.PrintLine;
import com.tigernet.backend.printing.core.PrintResponses;
import com.tigernet.backend.stocktake.entity.StockAdjustmentEntity;
import com.tigernet.backend.stocktake.entity.StocktakeDetailEntity;
import com.tigernet.backend.stocktake.entity.StocktakeEntity;
import com.tigernet.backend.stocktake.repository.StockAdjustmentRepository;
import com.tigernet.backend.stocktake.repository.StocktakeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Biên bản kiểm kê 05-VT (TT200) và phiếu điều chỉnh tồn kho / xử lý hàng hư hỏng, mất (mẫu nội bộ). */
@Service
@RequiredArgsConstructor
public class StocktakePrintService {
    private final StocktakeRepository repository;
    private final StockAdjustmentRepository adjustmentRepository;
    private final PdfRenderService renderer;

    /** Một dòng bảng 05-VT. Các số lượng thừa/thiếu/hư hỏng để null khi bằng 0 để ô trống như mẫu giấy. */
    public record StocktakeRow(int stt, String name, String code, String unit, BigDecimal unitPrice,
                               int bookQty, BigDecimal bookAmount, int actualQty, BigDecimal actualAmount,
                               Integer surplusQty, BigDecimal surplusAmount, Integer shortQty, BigDecimal shortAmount,
                               Integer goodQty, Integer damagedQty, String note) {
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> print(Integer id, String template, String format, boolean download) {
        boolean adjustment;
        if (template == null || template.isBlank() || "05-VT".equalsIgnoreCase(template)) {
            adjustment = false;
        } else if ("adjustment".equalsIgnoreCase(template)) {
            adjustment = true;
        } else {
            throw new AppException(ErrorCode.PRINT_TEMPLATE_INVALID);
        }
        StocktakeEntity s = repository.findById(id)
                .orElseThrow(() -> new AppException(ErrorCode.STOCKTAKE_NOT_FOUND));
        List<StockAdjustmentEntity> adjustments = adjustmentRepository.search(id, null, null);

        Map<String, Object> m = new HashMap<>();
        m.put("code", s.getStocktakeCode());
        m.put("date", s.getStocktakeDate());
        m.put("warehouseName", s.getWarehouse().getWarehouseName());
        m.put("warehouseAddress", s.getWarehouse().getAddress());
        m.put("storekeeper", s.getWarehouse().getManager() != null ? s.getWarehouse().getManager().getFullName() : null);
        m.put("createdBy", s.getCreatedBy() != null ? s.getCreatedBy().getFullName() : null);

        if (adjustment) {
            m.put("pages", adjustmentPages(s, adjustments));
            return PrintResponses.of(renderer, "dieu-chinh-ton-kho", m,
                    "Bien_ban_dieu_chinh_ton_kho_" + s.getStocktakeCode(), format, download);
        }

        // lý do điều chỉnh theo sản phẩm: dòng nào ghi "hỏng" thì tính là hàng mất phẩm chất
        Map<Integer, Integer> damaged = new HashMap<>();
        for (StockAdjustmentEntity a : adjustments) {
            if (a.getAdjustQty() < 0 && a.getReason() != null && a.getReason().toLowerCase(Locale.ROOT).contains("hỏng")) {
                damaged.merge(a.getProduct().getId(), -a.getAdjustQty(), Integer::sum);
            }
        }
        List<StocktakeRow> rows = new ArrayList<>();
        BigDecimal book = BigDecimal.ZERO, actual = BigDecimal.ZERO, surplus = BigDecimal.ZERO, shortage = BigDecimal.ZERO;
        int surplusQty = 0, shortQty = 0, goodQty = 0, damagedQty = 0;
        int stt = 1;
        for (StocktakeDetailEntity d : s.getDetails()) {
            // DB chưa có giá vốn: tạm dùng giá nhập mặc định của sản phẩm
            BigDecimal price = MoneyUtils.orZero(d.getProduct().getDefaultPriceIn());
            int diff = d.getActualQty() - d.getSystemQty();
            BigDecimal bookAmt = price.multiply(BigDecimal.valueOf(d.getSystemQty()));
            BigDecimal actAmt = price.multiply(BigDecimal.valueOf(d.getActualQty()));
            BigDecimal diffAmt = price.multiply(BigDecimal.valueOf(Math.abs(diff)));
            int bad = damaged.getOrDefault(d.getProduct().getId(), 0);
            book = book.add(bookAmt);
            actual = actual.add(actAmt);
            if (diff > 0) {
                surplus = surplus.add(diffAmt);
                surplusQty += diff;
            } else if (diff < 0) {
                shortage = shortage.add(diffAmt);
                shortQty -= diff;
            }
            goodQty += d.getActualQty();
            damagedQty += bad;
            rows.add(new StocktakeRow(stt++, d.getProduct().getProductName(), PrintLine.productCode(d.getProduct()),
                    d.getProduct().getUnit(), price, d.getSystemQty(), bookAmt, d.getActualQty(), actAmt,
                    diff > 0 ? diff : null, diff > 0 ? diffAmt : null,
                    diff < 0 ? -diff : null, diff < 0 ? diffAmt : null,
                    d.getActualQty(), bad > 0 ? bad : null, null));
        }
        m.put("rows", rows);
        m.put("bookTotal", book);
        m.put("actualTotal", actual);
        m.put("surplusTotal", surplus);
        m.put("shortTotal", shortage);
        m.put("bookQtyTotal", s.getDetails().stream().mapToInt(StocktakeDetailEntity::getSystemQty).sum());
        m.put("actualQtyTotal", s.getDetails().stream().mapToInt(StocktakeDetailEntity::getActualQty).sum());
        m.put("surplusQtyTotal", surplusQty);
        m.put("shortQtyTotal", shortQty);
        m.put("goodQtyTotal", goodQty);
        m.put("damagedQtyTotal", damagedQty);
        return PrintResponses.of(renderer, "05-VT_bien_ban_kiem_ke", m,
                "Bien_ban_kiem_ke_" + s.getStocktakeCode(), format, download);
    }

    /** Một trang "Biên bản điều chỉnh tồn kho" cho mỗi dòng điều chỉnh. */
    public record AdjustmentPage(String number, String productCode, String productName, String unit,
                                 Integer before, Integer after, String reason, int dotLines, LocalDate date) {
    }

    // ô "Lý do điều chỉnh" của bản gốc có 4 dòng chấm; khoảng 85 ký tự cỡ 13pt trên một dòng A5 ngang
    static final int REASON_LINES = 4;
    static final int REASON_CHARS_PER_LINE = 85;

    static int dotLines(String reason) {
        if (reason == null || reason.isBlank()) {
            return REASON_LINES;
        }
        int used = (reason.strip().length() + REASON_CHARS_PER_LINE - 1) / REASON_CHARS_PER_LINE;
        return Math.max(1, REASON_LINES - used);
    }

    private List<AdjustmentPage> adjustmentPages(StocktakeEntity s, List<StockAdjustmentEntity> adjustments) {
        Map<Integer, StocktakeDetailEntity> byProduct = new HashMap<>();
        for (StocktakeDetailEntity d : s.getDetails()) {
            byProduct.put(d.getProduct().getId(), d);
        }
        List<StockAdjustmentEntity> sorted = new ArrayList<>(adjustments);
        sorted.sort((a, b) -> Integer.compare(a.getId(), b.getId()));
        List<AdjustmentPage> pages = new ArrayList<>();
        int stt = 1;
        for (StockAdjustmentEntity a : sorted) {
            StocktakeDetailEntity d = byProduct.get(a.getProduct().getId());
            // tồn sau điều chỉnh = số thực tế kiểm kê; tồn trước = sau − số điều chỉnh
            Integer after = d != null ? d.getActualQty() : null;
            Integer before = after != null ? after - a.getAdjustQty() : null;
            LocalDate date = a.getCreatedAt() != null ? a.getCreatedAt().toLocalDate() : null;
            pages.add(new AdjustmentPage(String.format("%s-ĐC%02d", s.getStocktakeCode(), stt++),
                    PrintLine.productCode(a.getProduct()), a.getProduct().getProductName(), a.getProduct().getUnit(),
                    before, after, a.getReason(), dotLines(a.getReason()), date));
        }
        if (pages.isEmpty()) {
            // chưa có điều chỉnh: in 1 biên bản trống để điền tay
            pages.add(new AdjustmentPage(s.getStocktakeCode() + "-ĐC", null, null, null, null, null, null,
                    REASON_LINES, null));
        }
        return pages;
    }
}
