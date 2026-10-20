package com.tigernet.backend.stocktake.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.stocktake.dto.response.StockAdjustmentResponse;
import com.tigernet.backend.stocktake.dto.response.StocktakeDetailResponse;
import com.tigernet.backend.stocktake.dto.response.StocktakeResponse;
import com.tigernet.backend.stocktake.entity.StockAdjustmentEntity;
import com.tigernet.backend.stocktake.entity.StocktakeDetailEntity;
import com.tigernet.backend.stocktake.entity.StocktakeEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface StocktakeMapper {
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "warehouseName", source = "warehouse.warehouseName")
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    StocktakeResponse toResponse(StocktakeEntity stocktake);

    List<StocktakeResponse> toResponses(List<StocktakeEntity> stocktakes);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unit", source = "product.unit")
    StocktakeDetailResponse toDetailResponse(StocktakeDetailEntity detail);

    @Mapping(target = "stocktakeId", source = "stocktake.id")
    @Mapping(target = "stocktakeCode", source = "stocktake.stocktakeCode")
    @Mapping(target = "warehouseId", source = "stocktake.warehouse.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    StockAdjustmentResponse toAdjustmentResponse(StockAdjustmentEntity adjustment);

    List<StockAdjustmentResponse> toAdjustmentResponses(List<StockAdjustmentEntity> adjustments);
}
