package com.tigernet.backend.warranty.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.outbound.entity.ExportReceiptDetailEntity;
import com.tigernet.backend.warranty.dto.response.WarrantyClaimResponse;
import com.tigernet.backend.warranty.dto.response.WarrantyLookupResponse;
import com.tigernet.backend.warranty.entity.WarrantyClaimEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface WarrantyMapper {
    @Mapping(target = "exportDetailId", source = "exportDetail.id")
    @Mapping(target = "exportReceiptId", source = "exportDetail.exportReceipt.id")
    @Mapping(target = "exportReceiptCode", source = "exportDetail.exportReceipt.receiptCode")
    @Mapping(target = "customerId", source = "exportDetail.exportReceipt.customer.id")
    @Mapping(target = "customerName", source = "exportDetail.exportReceipt.customer.party.fullName")
    @Mapping(target = "productId", source = "exportDetail.product.id")
    @Mapping(target = "productName", source = "exportDetail.product.productName")
    @Mapping(target = "serial", source = "claim", qualifiedByName = "claimSerial")
    @Mapping(target = "inventoryId", source = "inventory.id")
    @Mapping(target = "receivedWarehouseId", source = "receivedWarehouse.id")
    @Mapping(target = "receivedWarehouseName", source = "receivedWarehouse.warehouseName")
    @Mapping(target = "restockedWarehouseId", source = "restockedWarehouse.id")
    @Mapping(target = "handledByName", source = "handledBy.fullName")
    @Mapping(target = "warrantyExpiry", source = "exportDetail.warrantyExpiry")
    WarrantyClaimResponse toResponse(WarrantyClaimEntity claim);

    List<WarrantyClaimResponse> toResponses(List<WarrantyClaimEntity> claims);

    // inWarranty/inventoryId do service tính (cần ngày hiện tại và tra tồn)
    @Mapping(target = "exportDetailId", source = "id")
    @Mapping(target = "exportReceiptId", source = "exportReceipt.id")
    @Mapping(target = "exportReceiptCode", source = "exportReceipt.receiptCode")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "customerId", source = "exportReceipt.customer.id")
    @Mapping(target = "customerName", source = "exportReceipt.customer.party.fullName")
    @Mapping(target = "dateOut", source = "exportReceipt.dateOut")
    @Mapping(target = "inWarranty", ignore = true)
    @Mapping(target = "inventoryId", ignore = true)
    WarrantyLookupResponse toLookup(ExportReceiptDetailEntity detail);

    @org.mapstruct.Named("claimSerial")
    default String claimSerial(WarrantyClaimEntity claim) {
        if (claim.getInventory() != null && claim.getInventory().getSerial() != null) {
            return claim.getInventory().getSerial();
        }
        return claim.getExportDetail().getSerial();
    }
}
