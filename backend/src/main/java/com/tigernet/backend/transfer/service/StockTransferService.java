package com.tigernet.backend.transfer.service;

import com.tigernet.backend.audit.service.AuditLogService;
import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.transfer.dto.request.StockTransferCreationRequest;
import com.tigernet.backend.transfer.dto.request.StockTransferDetailRequest;
import com.tigernet.backend.transfer.dto.request.StockTransferUpdateRequest;
import com.tigernet.backend.transfer.dto.response.StockTransferResponse;
import com.tigernet.backend.transfer.entity.StockTransferDetailEntity;
import com.tigernet.backend.transfer.entity.StockTransferEntity;
import com.tigernet.backend.transfer.enums.TransferStatus;
import com.tigernet.backend.transfer.mapper.StockTransferMapper;
import com.tigernet.backend.transfer.repository.StockTransferRepository;
import com.tigernet.backend.user.service.CurrentUserService;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.service.WarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// DRAFT -> APPROVED (duyệt) và hủy khi chưa xuất không động vào tồn nên làm ở đây;
// APPROVED -> IN_TRANSIT (ship) -> RECEIVED (receive) sẽ đi qua StockService
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class StockTransferService {
    static final String CODE_TYPE = "CK";

    StockTransferRepository stockTransferRepository;
    StockTransferMapper stockTransferMapper;
    WarehouseService warehouseService;
    ProductService productService;
    InventoryService inventoryService;
    CurrentUserService currentUserService;
    AuditLogService auditLogService;

    public StockTransferResponse createTransfer(StockTransferCreationRequest request) {
        WarehouseEntity fromWarehouse = warehouseService.findActiveWarehouse(request.getFromWarehouseId());

        StockTransferEntity transfer = new StockTransferEntity();
        transfer.setTransferCode(nextTransferCode(fromWarehouse));
        transfer.setFromWarehouse(fromWarehouse);
        transfer.setToWarehouse(findDestination(fromWarehouse, request.getToWarehouseId()));
        transfer.setStatus(TransferStatus.DRAFT);
        transfer.setNote(request.getNote());
        transfer.setCreatedBy(currentUserService.getCurrentUser());
        transfer.setCreatedAt(LocalDateTime.now());
        replaceDetails(transfer, request.getDetails());

        return stockTransferMapper.toResponse(stockTransferRepository.save(transfer));
    }

    @Transactional(readOnly = true)
    public List<StockTransferResponse> getTransfers(Integer fromWarehouseId, Integer toWarehouseId,
                                                    TransferStatus status) {
        return stockTransferMapper.toResponses(stockTransferRepository.search(fromWarehouseId, toWarehouseId, status));
    }

    @Transactional(readOnly = true)
    public StockTransferResponse getTransfer(Integer transferId) {
        return stockTransferMapper.toResponse(findTransfer(transferId));
    }

    public StockTransferResponse updateTransfer(Integer transferId, StockTransferUpdateRequest request) {
        StockTransferEntity transfer = findTransferInStatus(transferId, TransferStatus.DRAFT);
        if (request.getToWarehouseId() != null) {
            transfer.setToWarehouse(findDestination(transfer.getFromWarehouse(), request.getToWarehouseId()));
        }
        if (request.getNote() != null) transfer.setNote(request.getNote());
        if (request.getDetails() != null) {
            replaceDetails(transfer, request.getDetails());
        }
        return stockTransferMapper.toResponse(stockTransferRepository.save(transfer));
    }

    // quản lý duyệt: chưa động vào tồn
    public StockTransferResponse approveTransfer(Integer transferId) {
        StockTransferEntity transfer = findTransferInStatus(transferId, TransferStatus.DRAFT);
        transfer.setStatus(TransferStatus.APPROVED);
        transfer.setApprovedBy(currentUserService.getCurrentUser());
        transfer.setApprovedAt(LocalDateTime.now());
        return stockTransferMapper.toResponse(stockTransferRepository.save(transfer));
    }

    // chỉ hủy khi hàng chưa rời kho nguồn (DRAFT/APPROVED)
    public StockTransferResponse cancelTransfer(Integer transferId) {
        StockTransferEntity transfer = findTransfer(transferId);
        if (transfer.getStatus() != TransferStatus.DRAFT && transfer.getStatus() != TransferStatus.APPROVED) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        transfer.setStatus(TransferStatus.CANCELLED);
        return stockTransferMapper.toResponse(stockTransferRepository.save(transfer));
    }

    public void deleteTransfer(Integer transferId) {
        StockTransferEntity transfer = findTransferInStatus(transferId, TransferStatus.DRAFT);
        auditLogService.log("DELETE", "Stock_Transfers", transfer.getId(), stockTransferMapper.toResponse(transfer));
        stockTransferRepository.delete(transfer);
    }

    private StockTransferEntity findTransfer(Integer transferId) {
        return stockTransferRepository.findById(transferId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_TRANSFER_NOT_FOUND));
    }

    private StockTransferEntity findTransferInStatus(Integer transferId, TransferStatus status) {
        StockTransferEntity transfer = findTransfer(transferId);
        if (transfer.getStatus() != status) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return transfer;
    }

    private WarehouseEntity findDestination(WarehouseEntity fromWarehouse, Integer toWarehouseId) {
        if (fromWarehouse.getId().equals(toWarehouseId)) {
            throw new AppException(ErrorCode.SAME_WAREHOUSE_TRANSFER);
        }
        return warehouseService.findActiveWarehouse(toWarehouseId);
    }

    private void replaceDetails(StockTransferEntity transfer, List<StockTransferDetailRequest> requests) {
        transfer.getDetails().clear();
        Set<Integer> inventoryIds = new HashSet<>();

        for (StockTransferDetailRequest line : requests) {
            StockTransferDetailEntity detail = new StockTransferDetailEntity();
            detail.setStockTransfer(transfer);
            detail.setProduct(productService.findProduct(line.getProductId()));
            detail.setQtySent(line.getQtySent());
            detail.setSerial(StringUtils.hasText(line.getSerial()) ? line.getSerial().trim() : null);
            detail.setNote(line.getNote());

            if (line.getInventoryId() != null) {
                if (!inventoryIds.add(line.getInventoryId())) throw new AppException(ErrorCode.DUPLICATE_SERIAL);
                // đơn vị/lô phải đang ở kho nguồn
                InventoryEntity inventory = inventoryService.findInventoryForLine(
                        line.getInventoryId(), line.getProductId(), transfer.getFromWarehouse().getId());
                detail.setInventory(inventory);
                if (detail.getSerial() == null) detail.setSerial(inventory.getSerial());
            }
            if (detail.getSerial() != null && line.getQtySent() != 1) {
                throw new AppException(ErrorCode.SERIAL_QUANTITY_INVALID);
            }
            transfer.getDetails().add(detail);
        }
    }

    private String nextTransferCode(WarehouseEntity fromWarehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, fromWarehouse.getWarehouseCode());
        String lastCode = stockTransferRepository.findFirstByTransferCodeStartingWithOrderByTransferCodeDesc(prefix)
                .map(StockTransferEntity::getTransferCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
