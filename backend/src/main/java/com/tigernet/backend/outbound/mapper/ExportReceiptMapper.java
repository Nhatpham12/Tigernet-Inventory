package com.tigernet.backend.outbound.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.outbound.dto.response.ExportReceiptDetailResponse;
import com.tigernet.backend.outbound.dto.response.ExportReceiptResponse;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface ExportReceiptMapper {
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.party.fullName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "warehouseName", source = "warehouse.warehouseName")
    @Mapping(target = "receiverId", source = "receiver.id")
    @Mapping(target = "vatId", source = "vat.id")
    @Mapping(target = "vat", source = "vat.vat")
    @Mapping(target = "vatRate", source = "vat.rate")
    @Mapping(target = "paymentStatusId", source = "paymentStatus.id")
    @Mapping(target = "paymentStatus", source = "paymentStatus.paymentStatus")
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    ExportReceiptResponse toResponse(ExportReceiptEntity receipt);

    List<ExportReceiptResponse> toResponses(List<ExportReceiptEntity> receipts);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unit", source = "product.unit")
    @Mapping(target = "inventoryId", source = "inventory.id")
    ExportReceiptDetailResponse toDetailResponse(ExportReceiptDetailEntity detail);
}
