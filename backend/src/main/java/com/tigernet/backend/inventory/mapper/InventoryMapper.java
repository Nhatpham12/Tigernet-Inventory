package com.tigernet.backend.inventory.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.inventory.dto.response.InventoryResponse;
import com.tigernet.backend.inventory.dto.response.StockMovementResponse;
import com.tigernet.backend.inventory.dto.response.StockSettingResponse;
import com.tigernet.backend.inventory.dto.response.StockSummaryResponse;
import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.entity.ProductWarehouseSettingEntity;
import com.tigernet.backend.inventory.entity.StockMovementEntity;
import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface InventoryMapper {

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "locationId", source = "location.id")
    @Mapping(target = "locationCode", source = "location.locationCode")
    @Mapping(target = "importDetailId", source = "importDetail.id")
    InventoryResponse toInventoryResponse(InventoryEntity inventory);

    List<InventoryResponse> toInventoryResponses(List<InventoryEntity> inventories);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    StockSummaryResponse toStockSummaryResponse(StockSummaryEntity stockSummary);

    List<StockSummaryResponse> toStockSummaryResponses(List<StockSummaryEntity> stockSummaries);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    StockMovementResponse toStockMovementResponse(StockMovementEntity movement);

    List<StockMovementResponse> toStockMovementResponses(List<StockMovementEntity> movements);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    StockSettingResponse toStockSettingResponse(ProductWarehouseSettingEntity setting);

    List<StockSettingResponse> toStockSettingResponses(List<ProductWarehouseSettingEntity> settings);
}
