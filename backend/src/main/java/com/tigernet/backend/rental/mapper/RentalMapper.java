package com.tigernet.backend.rental.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.rental.dto.response.RentalReceiptDetailResponse;
import com.tigernet.backend.rental.dto.response.RentalReceiptResponse;
import com.tigernet.backend.rental.dto.response.ReturnReceiptDetailResponse;
import com.tigernet.backend.rental.dto.response.ReturnReceiptResponse;
import com.tigernet.backend.rental.entity.RentalReceiptDetailEntity;
import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptDetailEntity;
import com.tigernet.backend.rental.entity.ReturnReceiptEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface RentalMapper {
    @Mapping(target = "customerId", source = "customer.id")
    @Mapping(target = "customerName", source = "customer.party.fullName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "warehouseName", source = "warehouse.warehouseName")
    @Mapping(target = "receiverId", source = "receiver.id")
    @Mapping(target = "paymentStatusId", source = "paymentStatus.id")
    @Mapping(target = "paymentStatus", source = "paymentStatus.paymentStatus")
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    @Mapping(target = "approvedById", source = "approvedBy.userId")
    @Mapping(target = "approvedByName", source = "approvedBy.fullName")
    RentalReceiptResponse toRentalResponse(RentalReceiptEntity receipt);

    List<RentalReceiptResponse> toRentalResponses(List<RentalReceiptEntity> receipts);

    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "unit", source = "product.unit")
    @Mapping(target = "inventoryId", source = "inventory.id")
    RentalReceiptDetailResponse toRentalDetailResponse(RentalReceiptDetailEntity detail);

    @Mapping(target = "rentalReceiptId", source = "rentalReceipt.id")
    @Mapping(target = "rentalReceiptCode", source = "rentalReceipt.receiptCode")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "warehouseName", source = "warehouse.warehouseName")
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    ReturnReceiptResponse toReturnResponse(ReturnReceiptEntity receipt);

    List<ReturnReceiptResponse> toReturnResponses(List<ReturnReceiptEntity> receipts);

    @Mapping(target = "rentalDetailId", source = "rentalDetail.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "inventoryId", source = "inventory.id")
    ReturnReceiptDetailResponse toReturnDetailResponse(ReturnReceiptDetailEntity detail);
}
