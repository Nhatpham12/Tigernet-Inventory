package com.tigernet.backend.printing.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.printing.core.PdfRenderService;
import com.tigernet.backend.printing.core.PrintParty;
import com.tigernet.backend.printing.core.PrintResponses;
import com.tigernet.backend.warranty.entity.WarrantyClaimEntity;
import com.tigernet.backend.warranty.repository.WarrantyClaimRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Biên bản tiếp nhận bảo hành – sửa chữa thiết bị (mẫu nội bộ).
 * Một phiếu bảo hành là một đơn vị hàng; nhiều phiếu của cùng một khách có thể in chung một biên bản.
 */
@Service
@RequiredArgsConstructor
public class WarrantyClaimPrintService {
    static final int MAX_CLAIMS = 50;

    private final WarrantyClaimRepository repository;
    private final PdfRenderService renderer;

    public record ClaimLine(int stt, String name, int quantity, String serial, String condition) {
    }

    @Transactional(readOnly = true)
    public ResponseEntity<byte[]> print(List<Integer> ids, String format, boolean download) {
        List<Integer> unique = ids == null ? List.of() : ids.stream().filter(Objects::nonNull).distinct().toList();
        if (unique.isEmpty() || unique.size() > MAX_CLAIMS) {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }
        List<WarrantyClaimEntity> claims = new ArrayList<>(repository.findAllById(unique));
        if (claims.size() != unique.size()) {
            throw new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND);
        }
        claims.sort(Comparator.comparing(WarrantyClaimEntity::getReceivedAt).thenComparing(WarrantyClaimEntity::getId));

        // in chung chỉ khi mọi phiếu cùng một khách hàng
        ExportReceiptEntity first = claims.get(0).getExportDetail().getExportReceipt();
        Integer customerId = first.getCustomer() != null ? first.getCustomer().getId() : null;
        for (WarrantyClaimEntity c : claims) {
            ExportReceiptEntity r = c.getExportDetail().getExportReceipt();
            if (!Objects.equals(customerId, r.getCustomer() != null ? r.getCustomer().getId() : null)) {
                throw new AppException(ErrorCode.INVALID_PARAMETER);
            }
        }

        PrintParty customer = PrintParty.of(first.getCustomer() == null ? null : first.getCustomer().getParty());
        List<ClaimLine> lines = new ArrayList<>();
        int stt = 1;
        for (WarrantyClaimEntity c : claims) {
            ExportReceiptDetailEntity d = c.getExportDetail();
            String serial = c.getInventory() != null && c.getInventory().getSerial() != null
                    ? c.getInventory().getSerial() : d.getSerial();
            // một phiếu bảo hành = một đơn vị hàng; hàng không serial thì lấy theo số lượng dòng bán
            int qty = serial != null && !serial.isBlank() ? 1 : (d.getQuantity() != null ? d.getQuantity() : 1);
            lines.add(new ClaimLine(stt++, d.getProduct().getProductName(), qty, serial, c.getNote()));
        }
        LocalDateTime receivedAt = claims.get(0).getReceivedAt();
        String code = claims.stream().map(WarrantyClaimEntity::getClaimCode).collect(Collectors.joining(", "));

        Map<String, Object> m = new HashMap<>();
        m.put("code", code);
        m.put("date", receivedAt);
        m.put("customer", customer);
        m.put("contact", ExportReceiptPrintService.receiver(first, customer));
        m.put("lines", lines);
        return PrintResponses.of(renderer, "bien_ban_tiep_nhan_bao_hanh", m,
                PrintResponses.fileName("Bien_ban_tiep_nhan_bao_hanh", receivedAt), format, download);
    }
}
