package com.tigernet.backend.stocktake.service;

import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.util.DocumentCodeGenerator;
import com.tigernet.backend.inventory.service.InventoryService;
import com.tigernet.backend.stocktake.dto.request.StocktakeCreationRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeDetailRequest;
import com.tigernet.backend.stocktake.dto.request.StocktakeItemsRequest;
import com.tigernet.backend.stocktake.dto.response.StockAdjustmentResponse;
import com.tigernet.backend.stocktake.dto.response.StocktakeResponse;
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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// lập phiếu kiểm kê và nhập số đếm khi còn DRAFT;
// chốt phiếu (COMPLETED) -> sinh Stock_Adjustments và áp vào tồn sẽ đi qua StockService
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

    public StocktakeResponse createStocktake(StocktakeCreationRequest request) {
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
        return stocktakeMapper.toResponses(stocktakeRepository.search(warehouseId, status,
                from != null ? from.atStartOfDay() : null,
                to != null ? to.plusDays(1).atStartOfDay() : null));
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

    public void deleteStocktake(Integer stocktakeId) {
        stocktakeRepository.delete(findDraftStocktake(stocktakeId));
    }

    @Transactional(readOnly = true)
    public List<StockAdjustmentResponse> getAdjustments(Integer stocktakeId, Integer warehouseId, Integer productId) {
        return stocktakeMapper.toAdjustmentResponses(stockAdjustmentRepository.search(stocktakeId, warehouseId, productId));
    }

    @Transactional(readOnly = true)
    public StockAdjustmentResponse getAdjustment(Integer adjustmentId) {
        return stocktakeMapper.toAdjustmentResponse(stockAdjustmentRepository.findById(adjustmentId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCK_ADJUSTMENT_NOT_FOUND)));
    }

    private StocktakeEntity findStocktake(Integer stocktakeId) {
        return stocktakeRepository.findById(stocktakeId)
                .orElseThrow(() -> new AppException(ErrorCode.STOCKTAKE_NOT_FOUND));
    }

    private StocktakeEntity findDraftStocktake(Integer stocktakeId) {
        StocktakeEntity stocktake = findStocktake(stocktakeId);
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
