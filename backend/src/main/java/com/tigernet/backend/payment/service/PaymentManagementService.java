package com.tigernet.backend.payment.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.enums.ImportStatus;
import com.tigernet.backend.inbound.repository.ImportReceiptRepository;
import com.tigernet.backend.inbound.service.ImportReceiptService;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import com.tigernet.backend.outbound.repository.ExportReceiptRepository;
import com.tigernet.backend.outbound.service.ExportReceiptService;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.service.PartyService;
import com.tigernet.backend.payment.dto.request.PaymentCancelRequest;
import com.tigernet.backend.payment.dto.request.PaymentCreationRequest;
import com.tigernet.backend.payment.dto.response.PaymentResponse;
import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentEntryStatus;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import com.tigernet.backend.payment.mapper.PaymentMapper;
import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptEntity;
import com.tigernet.backend.rental.repository.RentalReceiptRepository;
import com.tigernet.backend.rental.repository.ReturnReceiptRepository;
import com.tigernet.backend.salesreturn.entity.SalesReturnEntity;
import com.tigernet.backend.salesreturn.repository.SalesReturnRepository;
import com.tigernet.backend.supplierreturn.entity.SupplierReturnEntity;
import com.tigernet.backend.supplierreturn.repository.SupplierReturnRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

// màn Phiếu thu/chi: lập, xem, hủy. Tách khỏi PaymentService vì ExportReceiptService/ImportReceiptService
// đã phụ thuộc PaymentService (tránh vòng phụ thuộc). Paid/Remain của phiếu gốc do chính service của phiếu đó tính.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PaymentManagementService {
    PaymentService paymentService;
    PaymentMapper paymentMapper;
    PartyService partyService;
    ExportReceiptService exportReceiptService;
    ImportReceiptService importReceiptService;
    ExportReceiptRepository exportReceiptRepository;
    ImportReceiptRepository importReceiptRepository;
    RentalReceiptRepository rentalReceiptRepository;
    ReturnReceiptRepository returnReceiptRepository;
    SalesReturnRepository salesReturnRepository;
    SupplierReturnRepository supplierReturnRepository;
    AuditLogService auditLogService;

    public PaymentResponse createPayment(PaymentCreationRequest request) {
        PaymentReferenceType type = request.getReferenceType();
        Integer refId = request.getReferenceId();
        if (type == null || refId == null) { // Payments.Reference_* là NOT NULL
            throw new AppException(ErrorCode.PAYMENT_REFERENCE_INVALID);
        }
        PartyEntity party = partyService.findParty(request.getPartyId());
        BigDecimal amount = MoneyUtils.round(request.getAmount());

        String warehouseCode;
        String receiptCode = null; // số phiếu được thanh toán, ghi vào nhật ký
        if (type == PaymentReferenceType.Export_Receipts) {
            ExportReceiptEntity receipt = exportReceiptService.findReceipt(refId);
            // phiếu xuất: khách trả tiền (IN), đúng khách của phiếu
            if (request.getDirection() != PaymentDirection.IN || receipt.getCustomer() == null
                    || !Objects.equals(receipt.getCustomer().getParty().getId(), party.getId())) {
                throw new AppException(ErrorCode.PAYMENT_REFERENCE_INVALID);
            }
            if (receipt.getStatus() == ExportStatus.CANCELLED) throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
            exportReceiptService.adjustPaid(receipt, amount);
            warehouseCode = receipt.getWarehouse().getWarehouseCode();
            receiptCode = receipt.getReceiptCode();
        } else if (type == PaymentReferenceType.Import_Receipts) {
            ImportReceiptEntity receipt = importReceiptService.findReceipt(refId);
            // phiếu nhập: chi trả NCC (OUT), đúng bên bán của phiếu
            if (request.getDirection() != PaymentDirection.OUT || receipt.getSourceParty() == null
                    || !Objects.equals(receipt.getSourceParty().getId(), party.getId())) {
                throw new AppException(ErrorCode.PAYMENT_REFERENCE_INVALID);
            }
            if (receipt.getStatus() == ImportStatus.CANCELLED) throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
            importReceiptService.adjustPaid(receipt, amount);
            warehouseCode = receipt.getWarehouse().getWarehouseCode();
            receiptCode = receipt.getReceiptCode();
        } else {
            warehouseCode = otherReferenceWarehouse(type, refId, party);
        }

        PaymentEntity payment = paymentService.record(request.getDirection(), party, type, refId, amount,
                request.getMethod(), request.getNote(), warehouseCode, request.getPaidAt());
        Map<String, Object> detail = new LinkedHashMap<>();
        detail.put("paymentCode", payment.getPaymentCode());
        detail.put("amount", amount);
        // đối tượng là phiếu được thanh toán; loại tham chiếu khác thì dùng số phiếu thu/chi
        auditLogService.logDocument("PAYMENT", type.name(), refId,
                receiptCode != null ? receiptCode : payment.getPaymentCode(), detail);
        return toResponse(payment);
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> getPayments(PaymentDirection direction, Integer partyId,
                                             PaymentReferenceType referenceType, Integer referenceId,
                                             LocalDate from, LocalDate to) {
        return paymentService.search(direction, partyId, referenceType, referenceId, from, to).stream()
                .map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public PaymentResponse getPayment(Integer paymentId) {
        return toResponse(paymentService.findPayment(paymentId));
    }

    // hủy thay vì xóa; trừ lại Paid của phiếu gốc
    public PaymentResponse cancelPayment(Integer paymentId, PaymentCancelRequest request) {
        PaymentEntity payment = paymentService.findPayment(paymentId);
        if (payment.getStatus() == PaymentEntryStatus.CANCELLED) {
            throw new AppException(ErrorCode.PAYMENT_ALREADY_CANCELLED);
        }
        if (payment.getReferenceType() == PaymentReferenceType.Export_Receipts) {
            exportReceiptRepository.findById(payment.getReferenceId())
                    .filter(r -> r.getStatus() != ExportStatus.CANCELLED)
                    .ifPresent(r -> exportReceiptService.adjustPaid(r, payment.getAmount().negate()));
        } else if (payment.getReferenceType() == PaymentReferenceType.Import_Receipts) {
            importReceiptRepository.findById(payment.getReferenceId())
                    .filter(r -> r.getStatus() != ImportStatus.CANCELLED)
                    .ifPresent(r -> importReceiptService.adjustPaid(r, payment.getAmount().negate()));
        }
        payment.setStatus(PaymentEntryStatus.CANCELLED);
        paymentService.save(payment);
        auditLogService.logDocument("CANCEL", "Payments", payment.getId(), payment.getPaymentCode(),
                Collections.singletonMap("reason", request != null ? request.getReason() : null));
        return toResponse(payment);
    }

    private PaymentResponse toResponse(PaymentEntity payment) {
        PaymentResponse response = paymentMapper.toResponse(payment);
        response.setReferenceCode(referenceCode(payment.getReferenceType(), payment.getReferenceId()));
        return response;
    }

    private String referenceCode(PaymentReferenceType type, Integer id) {
        return switch (type) {
            case Import_Receipts -> importReceiptRepository.findById(id).map(ImportReceiptEntity::getReceiptCode).orElse(null);
            case Export_Receipts -> exportReceiptRepository.findById(id).map(ExportReceiptEntity::getReceiptCode).orElse(null);
            case Rental_Receipts -> rentalReceiptRepository.findById(id).map(r -> r.getReceiptCode()).orElse(null);
            default -> null;
        };
    }

    /**
     * Phiếu gốc khác (thuê, trả hàng thuê, trả hàng bán, trả NCC) không có cột Paid nên không cập nhật số đã trả,
     * nhưng phải có thật, đúng đối tác của phiếu và phiếu thuê chưa hủy. Trả về mã kho của phiếu (dùng cho số phiếu thu/chi).
     */
    private String otherReferenceWarehouse(PaymentReferenceType type, Integer id, PartyEntity party) {
        return switch (type) {
            case Rental_Receipts -> {
                RentalReceiptEntity r = rentalReceiptRepository.findById(id).orElseThrow(PaymentManagementService::badReference);
                requireParty(r.getCustomer().getParty(), party);
                if (PaymentStatusEntity.CANCELLED.equals(r.getPaymentStatus().getPaymentStatus())) {
                    throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
                }
                yield r.getWarehouse().getWarehouseCode();
            }
            case Return_Receipts -> {
                ReturnReceiptEntity r = returnReceiptRepository.findById(id).orElseThrow(PaymentManagementService::badReference);
                requireParty(r.getRentalReceipt().getCustomer().getParty(), party);
                yield r.getWarehouse().getWarehouseCode();
            }
            case Sales_Returns -> {
                SalesReturnEntity r = salesReturnRepository.findById(id).orElseThrow(PaymentManagementService::badReference);
                requireParty(r.getParty(), party);
                yield r.getWarehouse().getWarehouseCode();
            }
            case Supplier_Returns -> {
                SupplierReturnEntity r = supplierReturnRepository.findById(id).orElseThrow(PaymentManagementService::badReference);
                requireParty(r.getSupplier().getParty(), party);
                yield r.getWarehouse().getWarehouseCode();
            }
            default -> throw badReference(); // phiếu nhập/xuất đã xử lý ở createPayment
        };
    }

    private static void requireParty(PartyEntity expected, PartyEntity actual) {
        if (expected == null || !Objects.equals(expected.getId(), actual.getId())) {
            throw badReference();
        }
    }

    private static AppException badReference() {
        return new AppException(ErrorCode.PAYMENT_REFERENCE_INVALID);
    }
}
