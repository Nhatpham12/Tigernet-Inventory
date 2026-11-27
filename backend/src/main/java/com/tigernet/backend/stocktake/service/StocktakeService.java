package com.tigernet.backend.stocktake.service;

import com.tigernet.backend.warehouse.service.WarehouseAccessService;
import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.inventory.service.StockRef;
import com.tigernet.backend.inventory.service.StockService;
import com.tigernet.backend.stocktake.dto.request.StocktakeCompleteRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeCreationRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeDetailRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeItemsRequest;
import com.tigernet.backend.stocktake.dto.response.StockAdjustmentResponse;
import com.tigernet.backend.stocktake.dto.response.StocktakeResponse;
import com.tigernet.backend.stocktake.entity.StockAdjustmentEntity;
import com.tigernet.backend.stocktake.entity.StocktakeDetailEntity;
import com.tigernet.backend.stocktake.entity.StocktakeEntity;
import com.tigernet.backend.stocktake.enums.StocktakeStatus;
import com.tigernet.backend.stocktake.mapper.StocktakeMapper;
import com.tigernet.backend.stocktake.repository.StockAdjustmentRepository;
import com.tigernet.backend.stocktake.repository.StocktakeRepository;
import com.tigernet.backend.user.service.CurrentUserService;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.service.WarehouseService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

// lập phiếu kiểm kê và nhập số đếm khi còn DRAFT;
// chốt phiếu (COMPLETED) -> sinh Stock_Adjustments và áp chênh lệch vào tồn qua StockService
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class StocktakeService {
    static final String CODE_TYPE = "KK";

    StocktakeRepository stocktakeRepository;
    StockAdjustmentRepository stockAdjustmentRepository;
    StocktakeMapper stocktakeMapper;
    WarehouseService warehouseService;
    ProductService productService;
    InventoryService inventoryService;
    CurrentUserService currentUserService;
    WarehouseAccessService warehouseAccess;
    StockService stockService;

    public StocktakeResponse createStocktake(StocktakeCreationRequest request) {
        warehouseAccess.assertAccess(request.getWarehouseId());
        WarehouseEntity warehouse = warehouseService.findActiveWarehouse(request.getWarehouseId());

        StocktakeEntity stocktake = new StocktakeEntity();
        stocktake.setStocktakeCode(nextStocktakeCode(warehouse));
        stocktake.setWarehouse(warehouse);
        stocktake.setStocktakeDate(request.getStocktakeDate() != null ? request.getStocktakeDate() : LocalDateTime.now());
        stocktake.setStatus(StocktakeStatus.DRAFT);
        stocktake.setCreatedBy(currentUserService.getCurrentUser());
        replaceDetails(stocktake, request.getDetails());

        return stocktakeMapper.toResponse(stocktakeRepository.save(stocktake));
    }

    @Transactional(readOnly = true)
    public List<StocktakeResponse> getStocktakes(Integer warehouseId, StocktakeStatus status,
                                                 LocalDate from, LocalDate to) {
        return stocktakeMapper.toResponses(warehouseAccess.filter(stocktakeRepository.search(warehouseId, status,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null), warehouseId, StocktakeEntity::getWarehouse));
    }

    @Transactional(readOnly = true)
    public StocktakeResponse getStocktake(Integer stocktakeId) {
        return stocktakeMapper.toResponse(findStocktake(stocktakeId));
    }

    public StocktakeResponse updateItems(Integer stocktakeId, StocktakeItemsRequest request) {
        StocktakeEntity stocktake = findDraftStocktake(stocktakeId);
        if (request.getStocktakeDate() != null) stocktake.setStocktakeDate(request.getStocktakeDate());
        replaceDetails(stocktake, request.getDetails());
        return stocktakeMapper.toResponse(stocktakeRepository.save(stocktake));
    }

    /**
     * Chốt phiếu (QL-03): mỗi dòng chênh lệch sinh một Stock_Adjustments (adjustQty = actual − system, bắt buộc lý do)
     * và áp vào tồn khả dụng qua StockService (thừa: thêm lô; thiếu: bớt lô cũ trước), thẻ kho ghi theo phiếu điều chỉnh.
     * Hàng serial có chênh lệch thì chặn: phải đối chiếu từng serial (nhập bổ sung / xử lý đơn vị) chứ không cộng trừ số.
     * Dòng đi theo id SP tăng dần để không khóa chéo. Entity Stocktakes chưa có cột người/thời điểm duyệt nên chưa ghi.
     */
    public StocktakeResponse completeStocktake(Integer stocktakeId, StocktakeCompleteRequest request) {
        StocktakeEntity stocktake = findDraftStocktake(stocktakeId);
        Map<Integer, String> reasons = request != null && request.getReasons() != null
                ? request.getReasons() : Map.of();
        List<StocktakeDetailEntity> diffs = stocktake.getDetails().stream()
                .filter(d -> d.getDifference() != 0)
                .sorted(Comparator.comparing((StocktakeDetailEntity d) -> d.getProduct().getId()))
                .toList();
        for (StocktakeDetailEntity detail : diffs) {
            if (Boolean.TRUE.equals(detail.getProduct().getIsSerialized())) {
                throw new AppException(ErrorCode.STOCKTAKE_SERIAL_DIFFERENCE);
            }
            String reason = reasons.get(detail.getProduct().getId());
            if (reason == null || reason.isBlank()) {
                throw new AppException(ErrorCode.ADJUSTMENT_REASON_REQUIRED);
            }
        }
        LocalDateTime now = LocalDateTime.now();
        for (StocktakeDetailEntity detail : diffs) {
            StockAdjustmentEntity adjustment = new StockAdjustmentEntity();
            adjustment.setStocktake(stocktake);
            adjustment.setProduct(detail.getProduct());
            adjustment.setAdjustQty(detail.getDifference());
            adjustment.setReason(reasons.get(detail.getProduct().getId()).trim());
            adjustment.setCreatedAt(now);
            stockAdjustmentRepository.save(adjustment);
            stockService.adjust(detail.getProduct(), stocktake.getWarehouse(), detail.getDifference(),
                    StockRef.of(StockRef.ADJUSTMENT, adjustment.getId()));
        }
        stocktake.setStatus(StocktakeStatus.COMPLETED);
        return stocktakeMapper.toResponse(stocktakeRepository.save(stocktake));
    }

    public void deleteStocktake(Integer stocktakeId) {
        stocktakeRepository.delete(findDraftStocktake(stocktakeId));
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentResponse> getAdjustments(Integer stocktakeId, Integer warehouseId, Integer productId) {
        return stocktakeMapper.toAdjustmentResponses(warehouseAccess.filter(
                stockAdjustmentRepository.search(stocktakeId, warehouseId, productId),
                warehouseId, a -> a.getStocktake().getWarehouse()));
    }

    @Transactional(readOnly = true)
    public StockAdjustmentResponse getAdjustment(Integer adjustmentId) {
        StockAdjustmentEntity adjustment = stockAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_ADJUSTMENT_NOT_FOUND));
        warehouseAccess.assertAccess(adjustment.getStocktake().getWarehouse());
        return stocktakeMapper.toAdjustmentResponse(adjustment);
    }

    private StocktakeEntity findStocktake(Integer stocktakeId) {
        StocktakeEntity stocktake = stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCKTAKE_NOT_FOUND));
        warehouseAccess.assertAccess(stocktake.getWarehouse());
        return stocktake;
    }

    // khóa phiếu (FOR UPDATE) trước khi kiểm trạng thái: chốt 2 lần cùng lúc không điều chỉnh tồn 2 lần
    private StocktakeEntity findDraftStocktake(Integer stocktakeId) {
        StocktakeEntity stocktake = stocktakeRepository.findByIdForUpdate(stocktakeId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCKTAKE_NOT_FOUND));
        warehouseAccess.assertAccess(stocktake.getWarehouse());
        if (stocktake.getStatus() != StocktakeStatus.DRAFT) {
            throw new AppException(ErrorCode.RECEIPT_NOT_EDITABLE);
        }
        return stocktake;
    }

    // System_Qty lấy theo tồn hiện tại của kho phiếu; Difference = Actual − System
    private void replaceDetails(StocktakeEntity stocktake, List<StocktakeDetailRequest> requests) {
        stocktake.getDetails().clear();
        Set<Integer> productIds = new HashSet<>();

        for (StocktakeDetailRequest line : requests) {
            if (!productIds.add(line.getProductId())) {
                throw new AppException(ErrorCode.DUPLICATE_PRODUCT_LINE);
            }
            int systemQty = inventoryService.getQtyOnHand(line.getProductId(), stocktake.getWarehouse().getId());

            StocktakeDetailEntity detail = new StocktakeDetailEntity();
            detail.setStocktake(stocktake);
            detail.setProduct(productService.findProduct(line.getProductId()));
            detail.setSystemQty(systemQty);
            detail.setActualQty(line.getActualQty());
            detail.setDifference(line.getActualQty() - systemQty);
            stocktake.getDetails().add(detail);
        }
    }

    private String nextStocktakeCode(WarehouseEntity warehouse) {
        String prefix = DocumentCodeGenerator.prefix(CODE_TYPE, warehouse.getWarehouseCode());
        String lastCode = stocktakeRepository.findFirstByStocktakeCodeStartingWithOrderByStocktakeCodeDesc(prefix)
                .map(StocktakeEntity::getStocktakeCode)
                .orElse(null);
        return DocumentCodeGenerator.next(prefix, lastCode);
    }
}
