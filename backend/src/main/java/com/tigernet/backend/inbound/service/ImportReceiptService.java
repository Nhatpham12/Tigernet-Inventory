package com.tigernet.backend.inbound.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.entity.VatOptionEntity;
import com.tigernet.backend.catalog.service.LookupService;
import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inbound.dto.request.ImportReceiptCreationRequest;
import com.tigernet.backend.inbound.dto.request.ImportReceiptDetailRequest;
import com.tigernet.backend.inbound.dto.request.ImportReceiptUpdateRequest;
import com.tigernet.backend.inbound.dto.response.ImportReceiptResponse;
import com.tigernet.backend.inbound.entity.ImportReceiptDetailEntity;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inbound.mapper.ImportReceiptMapper;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.partner.service.PartyService;
import com.tigernet.backend.user.service.CurrentUserService;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.service.WarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// lập/sửa/hủy phiếu nhập khi còn DRAFT/PENDING; xác nhận nhập kho (CONFIRMED) sẽ đi qua StockService
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ImportReceiptService {
    static final String CODE_TYPE = "PN";

    ImportReceiptRepository importReceiptRepository;
    ImportReceiptMapper importReceiptMapper;
    WarehouseService warehouseService;
    PartyService partyService;
    ProductService productService;
    InventoryService inventoryService;
    LookupService lookupService;
    CurrentUserService currentUserService;
    AuditLogService auditLogService;

    public ImportReceiptResponse createReceipt(ImportReceiptCreationRequest request) {
        WarehouseEntity warehouse = warehouseService.findActiveWarehouse(request.getWarehouseId());

        ImportReceiptEntity receipt = new ImportReceiptEntity();
        receipt.setReceiptCode(nextReceiptCode(warehouse));
        receipt.setWarehouse(warehouse);
        receipt.setSourceParty(partyService.findParty(request.getSourcePartyId()));
        receipt.setDateIn(request.getDateIn() != null ? request.getDateIn() : LocalDateTime.now());
        receipt.setVat(request.getVatId() != null ? lookupService.findVatOption(request.getVatId()) : null);
        receipt.setStatus(editableStatus(request.getStatus(), ImportStatus.DRAFT));
        receipt.setNote(request.getNote());
        receipt.setCreatedBy(currentUserService.getCurrentUser());
        receipt.setCreatedAt(LocalDateTime.now());
        replaceDetails(receipt, request.getDetails());
        recalculateMoney(receipt);

        return importReceiptMapper.toResponse(importReceiptRepository.save(receipt));
    }

    @Transactional(readOnly = true)
    public List<ImportReceiptResponse> getReceipts(Integer warehouseId, ImportStatus status, Integer sourcePartyId,
                                                   LocalDate from, LocalDate to) {
        return importReceiptMapper.toResponses(importReceiptRepository.search(warehouseId, status, sourcePartyId,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null));
    }

    @Transactional(readOnly = true)
    public ImportReceiptResponse getReceipt(Integer receiptId) {
        return importReceiptMapper.toResponse(findReceipt(receiptId));
    }

    public ImportReceiptResponse updateReceipt(Integer receiptId, ImportReceiptUpdateRequest request) {
        ImportReceiptEntity receipt = findEditableReceipt(receiptId);
        if (request.getSourcePartyId() != null) {
            receipt.setSourceParty(partyService.findParty(request.getSourcePartyId()));
        }
        if (request.getDateIn() != null) receipt.setDateIn(request.getDateIn());
        if (request.getVatId() != null) receipt.setVat(lookupService.findVatOption(request.getVatId()));
        if (request.getStatus() != null) receipt.setStatus(editableStatus(request.getStatus(), receipt.getStatus()));
        if (request.getNote() != null) receipt.setNote(request.getNote());
        if (request.getDetails() != null) {
            replaceDetails(receipt, request.getDetails());
        }
        recalculateMoney(receipt); // VAT hoặc dòng chi tiết có thể đã đổi
        return importReceiptMapper.toResponse(importReceiptRepository.save(receipt));
    }

    // chỉ hủy được khi chưa nhập kho; phiếu giữ lại ở trạng thái CANCELLED (số phiếu không đứt quãng)
    public ImportReceiptResponse cancelReceipt(Integer receiptId, String reason) {
        ImportReceiptEntity receipt = findEditableReceipt(receiptId);
        receipt.setStatus(ImportStatus.CANCELLED);
        receipt.setPaymentStatus(lookupService.findPaymentStatus(PaymentStatusEntity.CANCELLED));
        receipt.setCancelledBy(currentUserService.getCurrentUser());
        receipt.setCancelledAt(LocalDateTime.now());
        receipt.setCancelReason(reason);
        auditLogService.log("CANCEL", "Import_Receipts", receipt.getId(), reason);
        return importReceiptMapper.toResponse(importReceiptRepository.save(receipt));
    }

    public ImportReceiptEntity findReceipt(Integer receiptId) {
        return importReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.IMPORT_RECEIPT_NOT_FOUND));
    }

    private ImportReceiptEntity findEditableReceipt(Integer receiptId) {
        ImportReceiptEntity receipt = findReceipt(receiptId);
        if (receipt.getStatus() == ImportStatus.CONFIRMED || receipt.getStatus() == ImportStatus.CANCELLED) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return receipt;
    }

    // qua API này chỉ được đặt DRAFT/PENDING; CONFIRMED phải qua bước xác nhận nhập kho, CANCELLED qua bước hủy phiếu
    private ImportStatus editableStatus(ImportStatus requested, ImportStatus fallback) {
        if (requested == null) {
            return fallback;
        }
        if (requested == ImportStatus.CONFIRMED || requested == ImportStatus.CANCELLED) {
            throw new AppException(ErrorCode.STATUS_NOT_ALLOWED);
        }
        return requested;
    }

    // thay toàn bộ dòng chi tiết và tính lại Line_Total; Total_Amount tính ở recalculateMoney
    private void replaceDetails(ImportReceiptEntity receipt, List<ImportReceiptDetailRequest> requests) {
        receipt.getDetails().clear();
        Set<String> serials = new HashSet<>();

        for (ImportReceiptDetailRequest line : requests) {
            String serial = StringUtils.hasText(line.getSerial()) ? line.getSerial().trim() : null;
            if (serial != null) {
                if (line.getQuantity() != 1) throw new AppException(ErrorCode.SERIAL_QUANTITY_INVALID);
                if (!serials.add(serial)) throw new AppException(ErrorCode.DUPLICATE_SERIAL);
                if (inventoryService.serialExists(serial)) throw new AppException(ErrorCode.SERIAL_EXISTED);
            }

            ImportReceiptDetailEntity detail = new ImportReceiptDetailEntity();
            detail.setImportReceipt(receipt);
            detail.setProduct(productService.findProduct(line.getProductId()));
            detail.setQuantity(line.getQuantity());
            detail.setPriceIn(MoneyUtils.round(line.getPriceIn()));
            detail.setSerial(serial);
            detail.setTrackingCode(line.getTrackingCode());
            detail.setLineTotal(MoneyUtils.lineTotal(line.getQuantity(), line.getPriceIn()));
            receipt.getDetails().add(detail);
        }
    }

    // Total = tổng Line_Total + VAT; Remain = Total − Paid; Payment_Status theo số đã trả
    private void recalculateMoney(ImportReceiptEntity receipt) {
        BigDecimal subtotal = receipt.getDetails().stream()
                .map(ImportReceiptDetailEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        VatOptionEntity vat = receipt.getVat();
        BigDecimal vatAmount = vat != null && vat.getRate() != null
                ? MoneyUtils.percentOf(subtotal, vat.getRate())
                : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(vatAmount);

        receipt.setTotalAmount(total);
        receipt.setRemain(total.subtract(receipt.getPaid()));

        String paymentStatus;
        if (receipt.getRemain().signum() == 0) {
            paymentStatus = PaymentStatusEntity.PAID;
        } else if (receipt.getPaid().signum() == 0) {
            paymentStatus = PaymentStatusEntity.UNPAID;
        } else {
            paymentStatus = PaymentStatusEntity.PARTIAL;
        }
        receipt.setPaymentStatus(lookupService.findPaymentStatus(paymentStatus));
    }

    private String nextReceiptCode(WarehouseEntity warehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, warehouse.getWarehouseCode());
        String lastCode = importReceiptRepository.findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(prefix)
                .map(ImportReceiptEntity::getReceiptCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
