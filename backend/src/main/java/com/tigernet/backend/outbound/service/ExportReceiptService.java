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
import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;
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
import com.tigernet.backend.warehouse.service.WarehouseAccessService;
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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Phiếu xuất: PENDING (lập/sửa, chỉ kiểm tồn khả dụng) → CONFIRMED (Quản lý duyệt, giữ hàng: IN_STOCK → RESERVED)
 * → EXPORTED (Thủ kho hoàn tất: RESERVED → SOLD). Hủy được khi PENDING/CONFIRMED (CONFIRMED thì trả lại hàng giữ),
 * nhưng phiếu đã thu tiền phải hủy phiếu thu trước. Mọi thay đổi tồn đi qua StockService.
 */
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
    WarehouseAccessService warehouseAccess;
    StockService stockService;

    public ExportReceiptResponse createReceipt(ExportReceiptCreationRequest request) {
        warehouseAccess.assertAccess(request.getWarehouseId());
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
        return exportReceiptMapper.toResponses(warehouseAccess.filter(exportReceiptRepository.search(warehouseId,
                customerId, status,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null), warehouseId, ExportReceiptEntity::getWarehouse));
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

    // Quản lý duyệt (QL-01): PENDING → CONFIRMED, giữ hàng cho phiếu; thiếu hàng (phiếu khác đã giữ trước) thì 409
    public ExportReceiptResponse confirmReceipt(Integer receiptId) {
        ExportReceiptEntity receipt = findReceiptInStatus(receiptId, ExportStatus.PENDING);
        moveStock(receipt, InventoryStatus.IN_STOCK, InventoryStatus.RESERVED);
        receipt.setStatus(ExportStatus.CONFIRMED);
        return exportReceiptMapper.toResponse(exportReceiptRepository.save(receipt));
    }

    // Thủ kho hoàn tất xuất (TK-11): CONFIRMED → EXPORTED, hàng đang giữ thành đã bán (ra khỏi kho)
    public ExportReceiptResponse completeReceipt(Integer receiptId) {
        ExportReceiptEntity receipt = findReceiptInStatus(receiptId, ExportStatus.CONFIRMED);
        moveStock(receipt, InventoryStatus.RESERVED, InventoryStatus.SOLD);
        receipt.setStatus(ExportStatus.EXPORTED);
        return exportReceiptMapper.toResponse(exportReceiptRepository.save(receipt));
    }

    // hủy phiếu PENDING/CONFIRMED: phiếu chuyển sang CANCELLED, không xóa (số phiếu không đứt quãng)
    public ExportReceiptResponse cancelReceipt(Integer receiptId, String reason) {
        ExportReceiptEntity receipt = findReceiptForUpdate(receiptId);
        if (receipt.getStatus() != ExportStatus.PENDING && receipt.getStatus() != ExportStatus.CONFIRMED) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        if (receipt.getPaid().signum() > 0) {
            throw new AppException(ErrorCode.RECEIPT_HAS_PAYMENTS); // tiền đã thu phải hủy phiếu thu trước
        }
        if (receipt.getStatus() == ExportStatus.CONFIRMED) {
            moveStock(receipt, InventoryStatus.RESERVED, InventoryStatus.IN_STOCK);
        }
        receipt.setStatus(ExportStatus.CANCELLED);
        receipt.setPaymentStatus(lookupService.findPaymentStatus(PaymentStatusEntity.CANCELLED));
        receipt.setCancelledBy(currentUserService.getCurrentUser());
        receipt.setCancelledAt(LocalDateTime.now());
        receipt.setCancelReason(reason);
        auditLogService.logDocument("CANCEL", "Export_Receipts", receipt.getId(), receipt.getReceiptCode(),
                Collections.singletonMap("reason", reason));
        return exportReceiptMapper.toResponse(exportReceiptRepository.save(receipt));
    }

    // cộng dồn Paid, tính lại Remain/Payment_Status; mỗi lần thu là một dòng Payments (và 1 dòng Audit_Logs PAYMENT)
    public ExportReceiptResponse recordPayment(Integer receiptId, PaymentRequest request) {
        ExportReceiptEntity receipt = findReceipt(receiptId);
        if (receipt.getStatus() == ExportStatus.CANCELLED) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        BigDecimal amount = MoneyUtils.round(request.getAmount());
        adjustPaid(receipt, amount);
        recordPaymentEntry(receipt, amount, request.getMethod(), request.getNote());

        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("amount", amount);
        detail.put("paidAfter", receipt.getPaid());
        detail.put("remainAfter", receipt.getRemain());
        detail.put("note", request.getNote());
        auditLogService.logDocument("PAYMENT", "Export_Receipts", receipt.getId(), receipt.getReceiptCode(), detail);

        return exportReceiptMapper.toResponse(exportReceiptRepository.save(receipt));
    }

    // cộng (delta > 0) hoặc trừ (delta < 0, khi hủy phiếu thu) Paid rồi tính lại Remain/Payment_Status;
    // PaymentManagementService dùng chung để phiếu thu lập từ màn Phiếu thu/chi cập nhật phiếu xuất giống luồng trên
    public ExportReceiptEntity adjustPaid(ExportReceiptEntity receipt, BigDecimal delta) {
        receipt.setPaid(receipt.getPaid().add(delta));
        recalculateMoney(receipt);
        return exportReceiptRepository.save(receipt);
    }

    private void recordPaymentEntry(ExportReceiptEntity receipt, BigDecimal amount,
                                    PaymentMethod method, String note) {
        paymentService.record(PaymentDirection.IN, receipt.getCustomer().getParty(),
                PaymentReferenceType.Export_Receipts, receipt.getId(), amount, method, note,
                receipt.getWarehouse().getWarehouseCode());
    }

    // mọi đường đọc/sửa/hủy/thu tiền theo id (kể cả từ phiếu thu, đổi trả) đều qua đây: chỉ phiếu thuộc kho được giao
    public ExportReceiptEntity findReceipt(Integer receiptId) {
        ExportReceiptEntity receipt = exportReceiptRepository.findById(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPORT_RECEIPT_NOT_FOUND));
        warehouseAccess.assertAccess(receipt.getWarehouse());
        return receipt;
    }

    private ExportReceiptEntity findPendingReceipt(Integer receiptId) {
        return findReceiptInStatus(receiptId, ExportStatus.PENDING);
    }

    private ExportReceiptEntity findReceiptInStatus(Integer receiptId, ExportStatus status) {
        ExportReceiptEntity receipt = findReceiptForUpdate(receiptId);
        if (receipt.getStatus() != status) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return receipt;
    }

    // khóa phiếu (FOR UPDATE) trước khi kiểm trạng thái: duyệt/xuất/hủy cùng lúc không đổi tồn 2 lần
    private ExportReceiptEntity findReceiptForUpdate(Integer receiptId) {
        ExportReceiptEntity receipt = exportReceiptRepository.findByIdForUpdate(receiptId)
                .orElseThrow(() -> new AppException(ErrorCode.EXPORT_RECEIPT_NOT_FOUND));
        warehouseAccess.assertAccess(receipt.getWarehouse());
        return receipt;
    }

    /**
     * Chuyển hàng của cả phiếu giữa 2 trạng thái: hàng serial theo từng đơn vị đã chọn,
     * hàng không serial gộp số lượng theo SP (lấy lô cũ trước). Đi theo id SP tăng dần để không khóa chéo.
     */
    private void moveStock(ExportReceiptEntity receipt, InventoryStatus from, InventoryStatus to) {
        StockRef ref = StockRef.of(StockRef.EXPORT, receipt.getId());
        Map<Integer, List<ExportReceiptDetailEntity>> byProduct = new TreeMap<>();
        for (ExportReceiptDetailEntity d : receipt.getDetails()) {
            byProduct.computeIfAbsent(d.getProduct().getId(), k -> new ArrayList<>()).add(d);
        }
        for (List<ExportReceiptDetailEntity> lines : byProduct.values()) {
            ProductEntity product = lines.get(0).getProduct();
            if (Boolean.TRUE.equals(product.getIsSerialized())) {
                for (ExportReceiptDetailEntity d : lines) {
                    if (d.getInventory() == null) {
                        throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH); // phiếu cũ chưa chọn serial
                    }
                    stockService.moveSerial(d.getInventory(), from, to, ref);
                }
            } else {
                int qty = lines.stream().mapToInt(ExportReceiptDetailEntity::getQuantity).sum();
                stockService.moveQty(product, receipt.getWarehouse(), qty, from, to, ref);
            }
        }
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

    /**
     * Thay toàn bộ dòng và kiểm tồn khả dụng (TK-10), chưa giữ hàng:
     * - hàng serial: mỗi dòng SL 1, chọn đơn vị bằng inventoryId hoặc serial; đơn vị phải đang IN_STOCK ở kho xuất.
     * - hàng không serial: không nhận serial; tổng SL theo SP không vượt Qty_On_Hand của kho.
     */
    private void replaceDetails(ExportReceiptEntity receipt, List<ExportReceiptDetailRequest> requests) {
        receipt.getDetails().clear();
        Set<Integer> inventoryIds = new HashSet<>();
        Map<Integer, Integer> bulkQty = new TreeMap<>();
        Integer warehouseId = receipt.getWarehouse().getId();

        for (ExportReceiptDetailRequest line : requests) {
            ExportReceiptDetailEntity detail = new ExportReceiptDetailEntity();
            detail.setExportReceipt(receipt);
            ProductEntity product = productService.findProduct(line.getProductId());
            detail.setProduct(product);
            detail.setQuantity(line.getQuantity());
            detail.setPriceOut(MoneyUtils.round(line.getPriceOut()));
            String serial = StringUtils.hasText(line.getSerial()) ? line.getSerial().trim() : null;

            if (Boolean.TRUE.equals(product.getIsSerialized())) {
                if (line.getQuantity() != 1) throw new AppException(ErrorCode.SERIAL_QUANTITY_INVALID);
                Integer inventoryId = line.getInventoryId() != null ? line.getInventoryId()
                        : serial != null ? inventoryService.findBySerial(serial).getId()
                        : null;
                if (inventoryId == null) throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH);
                if (!inventoryIds.add(inventoryId)) throw new AppException(ErrorCode.DUPLICATE_SERIAL);
                InventoryEntity unit = inventoryService.findInventoryForLine(inventoryId, product.getId(), warehouseId);
                if (unit.getStatus() != InventoryStatus.IN_STOCK) {
                    throw new AppException(ErrorCode.INVENTORY_STATUS_INVALID);
                }
                detail.setInventory(unit);
                detail.setSerial(unit.getSerial());
            } else {
                if (line.getInventoryId() != null || serial != null) {
                    throw new AppException(ErrorCode.SERIAL_COUNT_MISMATCH);
                }
                bulkQty.merge(product.getId(), line.getQuantity(), Integer::sum);
            }

            detail.setWarrantyMonths(line.getWarrantyMonths());
            detail.setLineTotal(MoneyUtils.lineTotal(line.getQuantity(), line.getPriceOut()));
            receipt.getDetails().add(detail);
        }
        bulkQty.forEach((productId, qty) -> {
            if (qty > inventoryService.getQtyOnHand(productId, warehouseId)) {
                throw new AppException(ErrorCode.INSUFFICIENT_STOCK);
            }
        });
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
