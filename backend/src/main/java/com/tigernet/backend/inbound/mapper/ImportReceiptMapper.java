package com.tigernet.backend.inbound.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.inbound.dto.response.ImportReceiptDetailResponse;
import com.tigernet.backend.inbound.dto.response.ImportReceiptResponse;
import com.tigernet.backend.inbound.entity.ImportReceiptDetailEntity;
import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

// request -> entity do service tự dựng vì gần hết là quan hệ và cột dẫn xuất
@Mapper(config = MapstructConfig.class)
public interface ImportReceiptMapper {
    @Mapping(target = "sourcePartyId", source = "sourceParty.id")
    @Mapping(target = "sourcePartyName", source = "sourceParty.fullName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "warehouseName", source = "warehouse.warehouseName")
    @Mapping(target = "vatId", source = "vat.id")
    @Mapping(target = "vat", source = "vat.vat")
    @Mapping(target = "vatRate", source = "vat.rate")
    @Mapping(target = "paymentStatusId", source = "paymentStatus.id")
    @Mapping(target = "paymentStatus", source = "paymentStatus.paymentStatus")
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    @Mapping(target = "cancelledById", source = "cancelledBy.userId")
    @Mapping(target = "cancelledByName", source = "cancelledBy.fullName")
    ImportReceiptResponse toResponse(ImportReceiptEntity receipt);

    List<ImportReceiptResponse> toResponses(List<ImportReceiptEntity> receipts);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unit", source = "product.unit")
    ImportReceiptDetailResponse toDetailResponse(ImportReceiptDetailEntity detail);
}
