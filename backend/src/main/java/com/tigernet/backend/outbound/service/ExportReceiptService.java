package com.tigernet.backend.outbound.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.entity.VatOptionEntity;
import com.tigernet.backend.catalog.service.LookupService;
import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.outbound.dto.request.ExportReceiptCreationRequest;
import com.tigernet.backend.outbound.dto.request.ExportReceiptDetailRequest;
import com.tigernet.backend.outbound.dto.request.ExportReceiptUpdateRequest;
import com.tigernet.backend.outbound.dto.request.PaymentRequest;
import com.tigernet.backend.outbound.dto.response.ExportReceiptResponse;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.outbound.mapper.ExportReceiptMapper;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import com.tigernet.backend.partner.entity.ReceiverEntity;
import com.tigernet.backend.partner.service.CustomerService;
import com.tigernet.backend.partner.service.ReceiverService;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentMethod;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import com.tigernet.backend.payment.service.PaymentService;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

// lập/sửa/hủy phiếu xuất khi còn PENDING, ghi nhận thu tiền;
// xác nhận / xuất kho (CONFIRMED, EXPORTED) sẽ đi qua StockService
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class ExportReceiptService {
    static final String CODE_TYPE = "PX";

    ExportReceiptRepository exportReceiptRepository;
    ExportReceiptMapper exportReceiptMapper;
    WarehouseService warehouseService;
    CustomerService customerService;
    ReceiverService receiverService;
    ProductService productService;
    InventoryService inventoryService;
    LookupService lookupService;
    CurrentUserService currentUserService;
    AuditLogService auditLogService;
    PaymentService paymentService;

    public ExportReceiptResponse createReceipt(ExportReceiptCreationRequest request) {
        WarehouseEntity warehouse = warehouseService.findActiveWarehouse(request.getWarehouseId());

        ExportReceiptEntity receipt = new ExportReceiptEntity();
        receipt.setReceiptCode(nextReceiptCode(warehouse));
        receipt.setWarehouse(warehouse);
        receipt.setCustomer(customerService.findCustomer(request.getCustomerId()));
        applyReceiver(receipt, request.getReceiverId(), request.getReceiverName(),
                request.getReceiverAddress(), request.getReceiverPhone());
        receipt.setDateOut(request.getDateOut() != null ? request.getDateOut() : LocalDateTime.now());
        receipt.setDeliveryDate(request.getDeliveryDate());
        receipt.setVat(request.getVatId() != null ? lookupService.findVatOption(request.getVatId()) : null);
        receipt.setDeposit(MoneyUtils.round(MoneyUtils.orZero(request.getDeposit())));
        receipt.setPaid(MoneyUtils.round(MoneyUtils.orZero(request.getPaid())));
        receipt.setStatus(ExportStatus.PENDING);
        receipt.setNote(request.getNote());
        receipt.setCreatedBy(currentUserService.getCurrentUser());
        receipt.setCreatedAt(LocalDateTime.now());
        replaceDetails(receipt, request.getDetails());
        recalculateMoney(receipt);

        ExportReceiptEntity saved = exportReceiptRepository.save(receipt);
        if (saved.getPaid().signum() > 0) {
            recordPaymentEntry(saved, saved.getPaid(), null, "Thu khi lập phiếu");
        }
        return exportReceiptMapper.toResponse(saved);
    }

    @Transactional(readOnly = true)
    public List<ExportReceiptResponse> getReceipts(Integer warehouseId, Integer customerId, ExportStatus status,
                                                   LocalDate from, LocalDate to) {
        return exportReceiptMapper.toResponses(exportReceiptRepository.search(warehouseId, customerId, status,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null));
    }

    @Transactional(readOnly = true)
    public ExportReceiptResponse getReceipt(Integer receiptId) {
        return exportReceiptMapper.toResponse(findReceipt(receiptId));
    }

    // chỉ sửa được khi chưa xuất kho (§3.4.1)
    public ExportReceiptResponse updateReceipt(Integer receiptId, ExportReceiptUpdateRequest request) {
        ExportReceiptEntity receipt = findPendingReceipt(receiptId);
        if (request.getCustomerId() != null) {
            receipt.setCustomer(customerService.findCustomer(request.getCustomerId()));
        }
        boolean receiverChanged = request.getReceiverId() != null || request.getReceiverName() != null
                || request.getReceiverAddress() != null || request.getReceiverPhone() != null;
        if (receiverChanged) {
            applyReceiver(receipt, request.getReceiverId(), request.getReceiverName(),
                    request.getReceiverAddress(), request.getReceiverPhone());
        }
        if (request.getDateOut() != null) receipt.setDateOut(request.getDateOut());
        if (request.getDeliveryDate() != null) receipt.setDeliveryDate(request.getDeliveryDate());
        if (request.getVatId() != null) receipt.setVat(lookupService.findVatOption(request.getVatId()));
        if (request.getDeposit() != null) receipt.setDeposit(MoneyUtils.round(request.getDeposit()));
        if (request.getNote() != null) receipt.setNote(request.getNote());
        if (request.getDetails() != null) {
            replaceDetails(receipt, request.getDetails());
        } else if (request.getDateOut() != null) {
            refreshWarranty(receipt); // hạn bảo hành tính từ Date_Out
        }
        recalculateMoney(receipt);
        return exportReceiptMapper.toResponse(exportReceiptRepository.save(receipt));
    }

    // hủy phiếu PENDING: phiếu chuyển sang CANCELLED, không xóa (số phiếu không đứt quãng)
    public ExportReceiptResponse cancelReceipt(Integer receiptId, String reason) {
        ExportReceiptEntity receipt = findPendingReceipt(receiptId);
        receipt.setStatus(ExportStatus.CANCELLED);
        receipt.setPaymentStatus(lookupService.findPaymentStatus(PaymentStatusEntity.CANCELLED));
        receipt.setCancelledBy(currentUserService.getCurrentUser());
        receipt.setCancelledAt(LocalDateTime.now());
        receipt.setCancelReason(reason);
        auditLogService.log("CANCEL", "Export_Receipts", receipt.getId(), reason);
        return exportReceiptMapper.toResponse(exportReceiptRepository.save(receipt));
    }

    // cộng dồn Paid, tính lại Remain/Payment_Status; mỗi lần thu là một dòng Payments (và 1 dòng Audit_Logs PAYMENT)
    public ExportReceiptResponse recordPayment(Integer receiptId, PaymentRequest request) {
        ExportReceiptEntity receipt = findReceipt(receiptId);
        if (receipt.getStatus() == ExportStatus.CANCELLED) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        BigDecimal amount = MoneyUtils.round(request.getAmount());
        receipt.setPaid(receipt.getPaid().add(amount));
        recalculateMoney(receipt);
        recordPaymentEntry(receipt, amount, request.getMethod(), request.getNote());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("amount", amount);
        detail.put("paidAfter", receipt.getPaid());
        detail.put("remainAfter", receipt.getRemain());
        detail.put("note", request.getNote());
        auditLogService.log("PAYMENT", "Export_Receipts", receipt.getId(), detail);

        return exportReceiptMapper.toResponse(exportReceiptRepository.save(receipt));
    }

    private void recordPaymentEntry(ExportReceiptEntity receipt, BigDecimal amount,
                                    PaymentMethod method, String note) {
        paymentService.record(PaymentDirection.IN, receipt.getCustomer().getParty(),
                PaymentReferenceType.Export_Receipts, receipt.getId(), amount, method, note,
                receipt.getWarehouse().getWarehouseCode());
    }

    public ExportReceiptEntity findReceipt(Integer receiptId) {
        return exportReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPORT_RECEIPT_NOT_FOUND));
    }

    private ExportReceiptEntity findPendingReceipt(Integer receiptId) {
        ExportReceiptEntity receipt = findReceipt(receiptId);
        if (receipt.getStatus() != ExportStatus.PENDING) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return receipt;
    }

    // chụp thông tin người nhận vào phiếu; giá trị gửi kèm request ưu tiên hơn thông tin đã lưu
    private void applyReceiver(ExportReceiptEntity receipt, Integer receiverId,
                               String name, String address, String phone) {
        ReceiverEntity receiver = receiverId != null
                ? receiverService.findReceiverOfCustomer(receiverId, receipt.getCustomer().getId())
                : null;
        receipt.setReceiver(receiver);
        receipt.setReceiverNameSnapshot(name != null ? name : receiver != null ? receiver.getReceiverName() : null);
        receipt.setReceiverAddressSnapshot(address != null ? address : receiver != null ? receiver.getAddress() : null);
        receipt.setReceiverPhoneSnapshot(phone != null ? phone : receiver != null ? receiver.getPhone() : null);
    }

    private void replaceDetails(ExportReceiptEntity receipt, List<ExportReceiptDetailRequest> requests) {
        receipt.getDetails().clear();
        Set<Integer> inventoryIds = new HashSet<>();

        for (ExportReceiptDetailRequest line : requests) {
            ExportReceiptDetailEntity detail = new ExportReceiptDetailEntity();
            detail.setExportReceipt(receipt);
            detail.setProduct(productService.findProduct(line.getProductId()));
            detail.setQuantity(line.getQuantity());
            detail.setPriceOut(MoneyUtils.round(line.getPriceOut()));
            detail.setSerial(StringUtils.hasText(line.getSerial()) ? line.getSerial().trim() : null);

            if (line.getInventoryId() != null) {
                if (!inventoryIds.add(line.getInventoryId())) throw new AppException(ErrorCode.DUPLICATE_SERIAL);
                InventoryEntity inventory = inventoryService.findInventoryForLine(
                        line.getInventoryId(), line.getProductId(), receipt.getWarehouse().getId());
                detail.setInventory(inventory);
                if (detail.getSerial() == null) detail.setSerial(inventory.getSerial());
            }
            if (detail.getSerial() != null && line.getQuantity() != 1) {
                throw new AppException(ErrorCode.SERIAL_QUANTITY_INVALID);
            }

            detail.setWarrantyMonths(line.getWarrantyMonths());
            detail.setLineTotal(MoneyUtils.lineTotal(line.getQuantity(), line.getPriceOut()));
            receipt.getDetails().add(detail);
        }
        refreshWarranty(receipt);
    }

    // Warranty_Expiry = Date_Out + Warranty_Months
    private void refreshWarranty(ExportReceiptEntity receipt) {
        LocalDate dateOut = receipt.getDateOut().toLocalDate();
        for (ExportReceiptDetailEntity detail : receipt.getDetails()) {
            detail.setWarrantyExpiry(detail.getWarrantyMonths() != null
                    ? dateOut.plusMonths(detail.getWarrantyMonths())
                    : null);
        }
    }

    // Total = tổng Line_Total + VAT; Remain = Total − Paid; Payment_Status theo số đã trả
    private void recalculateMoney(ExportReceiptEntity receipt) {
        BigDecimal subtotal = receipt.getDetails().stream()
                .map(ExportReceiptDetailEntity::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        VatOptionEntity vat = receipt.getVat();
        BigDecimal vatAmount = vat != null && vat.getRate() != null
                ? MoneyUtils.percentOf(subtotal, vat.getRate())
                : BigDecimal.ZERO;
        BigDecimal total = subtotal.add(vatAmount);

        if (receipt.getPaid().compareTo(total) > 0) {
            throw new AppException(ErrorCode.PAID_EXCEEDS_TOTAL);
        }
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
        String lastCode = exportReceiptRepository.findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(prefix)
                .map(ExportReceiptEntity::getReceiptCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
