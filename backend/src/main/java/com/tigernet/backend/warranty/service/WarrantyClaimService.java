package com.tigernet.backend.warranty.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.common.util.MoneyUtils;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.repository.InventoryRepository;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.user.service.CurrentUserService;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.service.WarehouseService;
import com.tigernet.backend.warranty.dto.request.WarrantyClaimCreationRequest;
import com.tigernet.backend.warranty.dto.request.WarrantyStatusRequest;
import com.tigernet.backend.warranty.dto.response.WarrantyClaimResponse;
import com.tigernet.backend.warranty.dto.response.WarrantyLookupResponse;
import com.tigernet.backend.warranty.entity.WarrantyClaimEntity;
import com.tigernet.backend.warranty.enums.WarrantyClaimStatus;
import com.tigernet.backend.warranty.enums.WarrantyResult;
import com.tigernet.backend.warranty.mapper.WarrantyMapper;
import com.tigernet.backend.warranty.repository.ExportLineLookupRepository;
import com.tigernet.backend.warranty.repository.WarrantyClaimRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

// tiếp nhận và theo dõi vòng đời bảo hành: RECEIVED -> SENT -> RETURNED -> CLOSED (hoặc RECEIVED -> REJECTED -> CLOSED).
// Chưa ghi biến động tồn kho (nhận máy vào kho, xuất đi bảo hành, nhập lại sau bảo hành): chờ StockService.
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class WarrantyClaimService {
    static final String CODE_TYPE = "BH";

    WarrantyClaimRepository warrantyClaimRepository;
    ExportLineLookupRepository exportLineLookupRepository;
    InventoryRepository inventoryRepository;
    WarrantyMapper warrantyMapper;
    WarehouseService warehouseService;
    CurrentUserService currentUserService;

    @Transactional(readOnly = true)
    public List<WarrantyLookupResponse> lookup(String serial, Integer exportReceiptId) {
        List<ExportReceiptDetailEntity> lines;
        if (StringUtils.hasText(serial)) {
            lines = exportLineLookupRepository.findBySerial(serial.trim()).stream().limit(1).toList();
        } else if (exportReceiptId != null) {
            lines = exportLineLookupRepository.findByExportReceipt(exportReceiptId);
        } else {
            throw new AppException(ErrorCode.INVALID_PARAMETER);
        }
        if (lines.isEmpty()) throw new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND);
        return lines.stream().map(this::toLookup).toList();
    }

    public WarrantyClaimResponse createClaim(WarrantyClaimCreationRequest request) {
        ExportReceiptDetailEntity detail = exportLineLookupRepository.findById(request.getExportDetailId())
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));
        WarehouseEntity warehouse = warehouseService.findActiveWarehouse(request.getReceivedWarehouseId());

        WarrantyClaimEntity claim = new WarrantyClaimEntity();
        claim.setClaimCode(nextClaimCode(warehouse));
        claim.setExportDetail(detail);
        claim.setInventory(request.getInventoryId() != null
                ? inventoryRepository.findById(request.getInventoryId())
                        .orElseThrow(() -> new AppException(ErrorCode.INVENTORY_NOT_FOUND))
                : findInventory(detail));
        claim.setReceivedWarehouse(warehouse);
        claim.setStatus(WarrantyClaimStatus.RECEIVED);
        claim.setReceivedAt(request.getReceivedAt() != null ? request.getReceivedAt() : LocalDateTime.now());
        claim.setNote(request.getNote());
        claim.setHandledBy(currentUserService.getCurrentUser());
        return warrantyMapper.toResponse(warrantyClaimRepository.save(claim));
    }

    @Transactional(readOnly = true)
    public List<WarrantyClaimResponse> getClaims(WarrantyClaimStatus status, Integer warehouseId,
                                                 Integer customerId, LocalDate from, LocalDate to) {
        return warrantyMapper.toResponses(warrantyClaimRepository.search(status, warehouseId, customerId,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null));
    }

    @Transactional(readOnly = true)
    public WarrantyClaimResponse getClaim(Integer claimId) {
        return warrantyMapper.toResponse(findClaim(claimId));
    }

    public WarrantyClaimResponse changeStatus(Integer claimId, WarrantyStatusRequest request) {
        WarrantyClaimEntity claim = findClaim(claimId);
        WarehouseEntity restocked = request.getRestockedWarehouseId() != null
                ? warehouseService.findActiveWarehouse(request.getRestockedWarehouseId())
                : null;
        applyStatus(claim, request, restocked, LocalDateTime.now());
        return warrantyMapper.toResponse(warrantyClaimRepository.save(claim));
    }

    // Phần thuần nghiệp vụ: kiểm tra chuyển trạng thái và ghi các mốc/kết quả tương ứng.
    static void applyStatus(WarrantyClaimEntity claim, WarrantyStatusRequest request,
                            WarehouseEntity restocked, LocalDateTime now) {
        WarrantyClaimStatus next = request.getStatus();
        if (!claim.getStatus().canMoveTo(next)) {
            throw new AppException(ErrorCode.WARRANTY_STATUS_INVALID);
        }
        switch (next) {
            case SENT -> claim.setSentAt(now);
            case RETURNED -> {
                claim.setReturnedAt(now);
                if (restocked != null) claim.setRestockedWarehouse(restocked);
            }
            case REJECTED -> claim.setResult(WarrantyResult.REJECTED);
            default -> { /* CLOSED: chỉ đổi trạng thái */ }
        }
        if (request.getResult() != null) claim.setResult(request.getResult());
        if (request.getCost() != null) claim.setCost(MoneyUtils.round(request.getCost()));
        if (request.getReplacementSerial() != null) claim.setReplacementSerial(request.getReplacementSerial());
        if (request.getNewWarrantyExpiry() != null) claim.setNewWarrantyExpiry(request.getNewWarrantyExpiry());
        if (request.getNote() != null) claim.setNote(request.getNote());
        claim.setStatus(next);
    }

    private WarrantyClaimEntity findClaim(Integer claimId) {
        return warrantyClaimRepository.findById(claimId)
                .orElseThrow(() -> new AppException(ErrorCode.WARRANTY_CLAIM_NOT_FOUND));
    }

    private InventoryEntity findInventory(ExportReceiptDetailEntity detail) {
        if (detail.getInventory() != null) return detail.getInventory();
        return StringUtils.hasText(detail.getSerial())
                ? inventoryRepository.findBySerial(detail.getSerial()).orElse(null)
                : null;
    }

    private WarrantyLookupResponse toLookup(ExportReceiptDetailEntity detail) {
        WarrantyLookupResponse response = warrantyMapper.toLookup(detail);
        InventoryEntity inventory = findInventory(detail);
        response.setInventoryId(inventory != null ? inventory.getId() : null);
        if (response.getSerial() == null && inventory != null) response.setSerial(inventory.getSerial());
        response.setInWarranty(detail.getWarrantyExpiry() != null
                && !detail.getWarrantyExpiry().isBefore(LocalDate.now()));
        return response;
    }

    private String nextClaimCode(WarehouseEntity warehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, warehouse.getWarehouseCode());
        String lastCode = warrantyClaimRepository.findFirstByClaimCodeStartingWithOrderByClaimCodeDesc(prefix)
                .map(WarrantyClaimEntity::getClaimCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
