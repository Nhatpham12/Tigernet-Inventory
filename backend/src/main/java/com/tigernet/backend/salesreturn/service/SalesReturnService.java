package com.tigernet.backend.salesreturn.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.service.ExportReceiptService;
import com.tigernet.backend.partner.service.PartyService;
import com.tigernet.backend.salesreturn.dto.request.SalesReturnCreationRequest;
import com.tigernet.backend.salesreturn.dto.request.SalesReturnDetailRequest;
import com.tigernet.backend.salesreturn.dto.request.SalesReturnUpdateRequest;
import com.tigernet.backend.salesreturn.dto.response.SalesReturnResponse;
import com.tigernet.backend.salesreturn.entity.ReturnFeePolicyEntity;
import com.tigernet.backend.salesreturn.entity.SalesReturnDetailEntity;
import com.tigernet.backend.salesreturn.entity.SalesReturnEntity;
import com.tigernet.backend.salesreturn.enums.ExchangeDirection;
import com.tigernet.backend.salesreturn.enums.FeeActionType;
import com.tigernet.backend.salesreturn.enums.ReturnType;
import com.tigernet.backend.salesreturn.enums.SalesReturnStatus;
import com.tigernet.backend.salesreturn.mapper.SalesReturnMapper;
import com.tigernet.backend.salesreturn.repository.SalesReturnRepository;
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
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

// lập/sửa/xóa phiếu đổi/trả khi còn DRAFT, tự tính phí theo Return_Fee_Policy;
// duyệt + nhập lại kho + sinh phiếu xuất đổi hàng sẽ đi qua StockService
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SalesReturnService {
    static final String CODE_TYPE = "TH";

    SalesReturnRepository salesReturnRepository;
    SalesReturnMapper salesReturnMapper;
    ReturnFeePolicyService policyService;
    ExportReceiptService exportReceiptService;
    PartyService partyService;
    WarehouseService warehouseService;
    InventoryService inventoryService;
    CurrentUserService currentUserService;
    AuditLogService auditLogService;

    public SalesReturnResponse createSalesReturn(SalesReturnCreationRequest request) {
        ExportReceiptEntity exportReceipt = exportReceiptService.findReceipt(request.getExportReceiptId());
        WarehouseEntity warehouse = warehouseService.findActiveWarehouse(request.getWarehouseId());

        SalesReturnEntity salesReturn = new SalesReturnEntity();
        salesReturn.setReceiptCode(nextReceiptCode(warehouse));
        salesReturn.setExportReceipt(exportReceipt);
        salesReturn.setWarehouse(warehouse);
        salesReturn.setCreatedBy(currentUserService.getCurrentUser());
        salesReturn.setCreatedAt(LocalDateTime.now());
        apply(salesReturn, request.getPartyId(), request.getReturnType(), request.getExchangeDirection(),
                request.getNewExportReceiptId(), request.getReturnDate(), request.getNote(), request.getDetails());

        return salesReturnMapper.toResponse(salesReturnRepository.save(salesReturn));
    }

    @Transactional(readOnly = true)
    public List<SalesReturnResponse> getSalesReturns(Integer warehouseId, Integer exportReceiptId,
                                                     ReturnType returnType, SalesReturnStatus status) {
        return salesReturnMapper.toResponses(
                salesReturnRepository.search(warehouseId, exportReceiptId, returnType, status));
    }

    @Transactional(readOnly = true)
    public SalesReturnResponse getSalesReturn(Integer salesReturnId) {
        return salesReturnMapper.toResponse(findSalesReturn(salesReturnId));
    }

    // phiếu nháp hoặc đã bị từ chối được sửa lại và tính phí lại từ đầu
    public SalesReturnResponse updateSalesReturn(Integer salesReturnId, SalesReturnUpdateRequest request) {
        SalesReturnEntity salesReturn = findEditableSalesReturn(salesReturnId);
        apply(salesReturn, request.getPartyId(), request.getReturnType(), request.getExchangeDirection(),
                request.getNewExportReceiptId(), request.getReturnDate(), request.getNote(), request.getDetails());
        return salesReturnMapper.toResponse(salesReturnRepository.save(salesReturn));
    }

    public void deleteSalesReturn(Integer salesReturnId) {
        SalesReturnEntity salesReturn = findEditableSalesReturn(salesReturnId);
        auditLogService.log("DELETE", "Sales_Returns", salesReturn.getId(), salesReturnMapper.toResponse(salesReturn));
        salesReturnRepository.delete(salesReturn);
    }

    private SalesReturnEntity findSalesReturn(Integer salesReturnId) {
        return salesReturnRepository.findById(salesReturnId)
                .orElseThrow(() -> new AppException(ErrorCode.SALES_RETURN_NOT_FOUND));
    }

    private SalesReturnEntity findEditableSalesReturn(Integer salesReturnId) {
        SalesReturnEntity salesReturn = findSalesReturn(salesReturnId);
        if (salesReturn.getStatus() != SalesReturnStatus.DRAFT && salesReturn.getStatus() != SalesReturnStatus.REJECTED) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return salesReturn;
    }

    // gán toàn bộ thông tin phiếu rồi tính lại số ngày, phí, tiền hoàn
    private void apply(SalesReturnEntity salesReturn, Integer partyId, ReturnType returnType,
                       ExchangeDirection exchangeDirection, Integer newExportReceiptId,
                       LocalDateTime returnDate, String note, List<SalesReturnDetailRequest> details) {
        ExportReceiptEntity exportReceipt = salesReturn.getExportReceipt();

        if (returnType == ReturnType.EXCHANGE && exchangeDirection == null) {
            throw new AppException(ErrorCode.EXCHANGE_DIRECTION_REQUIRED);
        }
        salesReturn.setParty(partyId != null ? partyService.findParty(partyId) : exportReceipt.getCustomer().getParty());
        salesReturn.setReturnType(returnType);
        salesReturn.setExchangeDirection(returnType == ReturnType.EXCHANGE ? exchangeDirection : null);
        salesReturn.setNewExportReceipt(resolveNewExport(exportReceipt, returnType, newExportReceiptId));
        salesReturn.setReturnDate(returnDate != null ? returnDate : LocalDateTime.now());
        salesReturn.setNote(note);
        replaceDetails(salesReturn, details);
        calculateFee(salesReturn);
    }

    private ExportReceiptEntity resolveNewExport(ExportReceiptEntity original, ReturnType returnType,
                                                 Integer newExportReceiptId) {
        if (returnType != ReturnType.EXCHANGE || newExportReceiptId == null) {
            return null;
        }
        if (newExportReceiptId.equals(original.getId())) {
            throw new AppException(ErrorCode.NEW_EXPORT_INVALID);
        }
        return exportReceiptService.findReceipt(newExportReceiptId);
    }

    private void replaceDetails(SalesReturnEntity salesReturn, List<SalesReturnDetailRequest> requests) {
        Map<Integer, ExportReceiptDetailEntity> exportLines = new HashMap<>();
        salesReturn.getExportReceipt().getDetails().forEach(line -> exportLines.put(line.getId(), line));
        Map<Integer, Integer> returningNow = new HashMap<>();

        salesReturn.getDetails().clear();
        BigDecimal originalValue = BigDecimal.ZERO;

        for (SalesReturnDetailRequest line : requests) {
            ExportReceiptDetailEntity exportLine = exportLines.get(line.getExportDetailId());
            if (exportLine == null) {
                throw new AppException(ErrorCode.EXPORT_DETAIL_INVALID);
            }
            int returned = returningNow.merge(exportLine.getId(), line.getQuantity(), Integer::sum)
                    + salesReturnRepository.sumReturnedQuantity(exportLine.getId(), salesReturn.getId());
            if (returned > exportLine.getQuantity()) {
                throw new AppException(ErrorCode.RETURN_QUANTITY_EXCEEDED);
            }

            SalesReturnDetailEntity detail = new SalesReturnDetailEntity();
            detail.setSalesReturn(salesReturn);
            detail.setExportDetail(exportLine);
            detail.setProduct(exportLine.getProduct());
            detail.setInventory(line.getInventoryId() != null
                    ? inventoryService.findInventoryOfProduct(line.getInventoryId(), exportLine.getProduct().getId())
                    : exportLine.getInventory());
            detail.setQuantity(line.getQuantity());
            detail.setSerial(StringUtils.hasText(line.getSerial()) ? line.getSerial().trim() : exportLine.getSerial());
            detail.setReturnCondition(line.getReturnCondition());
            detail.setRestockStatus(null); // ghi khi hàng thực sự nhập lại kho
            salesReturn.getDetails().add(detail);

            // giá trị gốc tính theo giá bán trên dòng bán gốc
            originalValue = originalValue.add(MoneyUtils.lineTotal(line.getQuantity(), exportLine.getPriceOut()));
        }
        salesReturn.setOriginalValue(originalValue);
    }

    // quy tắc: quá bậc cuối (1 tháng) hoặc bậc bị khóa -> REJECTED;
    // Fee = Original × %; Refund = Original − Fee (đổi hàng trừ thêm giá trị phiếu xuất mới)
    private void calculateFee(SalesReturnEntity salesReturn) {
        ExportReceiptEntity exportReceipt = salesReturn.getExportReceipt();
        BigDecimal originalValue = salesReturn.getOriginalValue();

        Integer days = null;
        if (exportReceipt.getDeliveryDate() != null) {
            days = (int) Math.max(0, ChronoUnit.DAYS.between(
                    exportReceipt.getDeliveryDate().toLocalDate(), salesReturn.getReturnDate().toLocalDate()));
        }
        salesReturn.setDaysSinceDelivery(days);
        salesReturn.setStatus(SalesReturnStatus.DRAFT);
        salesReturn.setPolicy(null);
        salesReturn.setFeePercent(BigDecimal.ZERO);
        salesReturn.setFeeAmount(BigDecimal.ZERO);

        // bảo hành không tính phí và không hoàn tiền
        if (salesReturn.getReturnType() == ReturnType.WARRANTY) {
            salesReturn.setRefundAmount(BigDecimal.ZERO);
            return;
        }
        if (days == null) {
            throw new AppException(ErrorCode.DELIVERY_DATE_MISSING);
        }

        Optional<ReturnFeePolicyEntity> policy = policyService.findApplicablePolicy(
                feeActionType(salesReturn), days, salesReturn.getReturnDate().toLocalDate());
        if (policy.isEmpty() || !Boolean.TRUE.equals(policy.get().getIsAllowed())) {
            salesReturn.setPolicy(policy.orElse(null));
            salesReturn.setStatus(SalesReturnStatus.REJECTED);
            salesReturn.setRefundAmount(BigDecimal.ZERO);
            return;
        }

        BigDecimal feePercent = policy.get().getFeePercent();
        BigDecimal feeAmount = MoneyUtils.percentOf(originalValue, feePercent);
        BigDecimal refund = originalValue.subtract(feeAmount);
        if (salesReturn.getNewExportReceipt() != null) {
            refund = refund.subtract(salesReturn.getNewExportReceipt().getTotalAmount());
        }
        salesReturn.setPolicy(policy.get());
        salesReturn.setFeePercent(feePercent);
        salesReturn.setFeeAmount(feeAmount);
        salesReturn.setRefundAmount(refund);
    }

    private FeeActionType feeActionType(SalesReturnEntity salesReturn) {
        if (salesReturn.getReturnType() == ReturnType.REFUND) {
            return FeeActionType.REFUND;
        }
        return salesReturn.getExchangeDirection() == ExchangeDirection.LOWER
                ? FeeActionType.EXCHANGE_LOWER
                : FeeActionType.EXCHANGE_EQUAL_HIGHER;
    }

    private String nextReceiptCode(WarehouseEntity warehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, warehouse.getWarehouseCode());
        String lastCode = salesReturnRepository.findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(prefix)
                .map(SalesReturnEntity::getReceiptCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
