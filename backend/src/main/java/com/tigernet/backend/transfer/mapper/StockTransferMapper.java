package com.tigernet.backend.transfer.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.transfer.dto.response.StockTransferDetailResponse;
import com.tigernet.backend.transfer.dto.response.StockTransferResponse;
import com.tigernet.backend.transfer.entity.StockTransferDetailEntity;
import com.tigernet.backend.transfer.entity.StockTransferEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface StockTransferMapper {
    @Mapping(target = "fromWarehouseId", source = "fromWarehouse.id")
    @Mapping(target = "fromWarehouseCode", source = "fromWarehouse.warehouseCode")
    @Mapping(target = "toWarehouseId", source = "toWarehouse.id")
    @Mapping(target = "toWarehouseCode", source = "toWarehouse.warehouseCode")
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    @Mapping(target = "approvedById", source = "approvedBy.userId")
    @Mapping(target = "approvedByName", source = "approvedBy.fullName")
    @Mapping(target = "receivedById", source = "receivedBy.userId")
    @Mapping(target = "receivedByName", source = "receivedBy.fullName")
    StockTransferResponse toResponse(StockTransferEntity transfer);

    List<StockTransferResponse> toResponses(List<StockTransferEntity> transfers);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unit", source = "product.unit")
    @Mapping(target = "inventoryId", source = "inventory.id")
    StockTransferDetailResponse toDetailResponse(StockTransferDetailEntity detail);
}
