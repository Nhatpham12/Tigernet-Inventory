package com.tigernet.backend.salesreturn.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.salesreturn.dto.request.ReturnFeePolicyRequest;
import com.tigernet.backend.salesreturn.dto.response.ReturnFeePolicyResponse;
import com.tigernet.backend.salesreturn.dto.response.SalesReturnDetailResponse;
import com.tigernet.backend.salesreturn.dto.response.SalesReturnResponse;
import com.tigernet.backend.salesreturn.entity.ReturnFeePolicyEntity;
import com.tigernet.backend.salesreturn.entity.SalesReturnDetailEntity;
import com.tigernet.backend.salesreturn.entity.SalesReturnEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface SalesReturnMapper {
    @Mapping(target = "exportReceiptId", source = "exportReceipt.id")
    @Mapping(target = "exportReceiptCode", source = "exportReceipt.receiptCode")
    @Mapping(target = "partyId", source = "party.id")
    @Mapping(target = "partyName", source = "party.fullName")
    @Mapping(target = "warehouseId", source = "warehouse.id")
    @Mapping(target = "warehouseCode", source = "warehouse.warehouseCode")
    @Mapping(target = "policyId", source = "policy.id")
    @Mapping(target = "policyTierName", source = "policy.tierName")
    @Mapping(target = "newExportReceiptId", source = "newExportReceipt.id")
    @Mapping(target = "newExportReceiptCode", source = "newExportReceipt.receiptCode")
    @Mapping(target = "createdById", source = "createdBy.userId")
    @Mapping(target = "createdByName", source = "createdBy.fullName")
    SalesReturnResponse toResponse(SalesReturnEntity salesReturn);

    List<SalesReturnResponse> toResponses(List<SalesReturnEntity> salesReturns);

    @Mapping(target = "exportDetailId", source = "exportDetail.id")
    @Mapping(target = "productId", source = "product.id")
    @Mapping(target = "productName", source = "product.productName")
    @Mapping(target = "inventoryId", source = "inventory.id")
    SalesReturnDetailResponse toDetailResponse(SalesReturnDetailEntity detail);

    @Mapping(target = "id", ignore = true)
    ReturnFeePolicyEntity toPolicy(ReturnFeePolicyRequest request);

    @Mapping(target = "id", ignore = true)
    void updatePolicy(@MappingTarget ReturnFeePolicyEntity policy, ReturnFeePolicyRequest request);

    ReturnFeePolicyResponse toPolicyResponse(ReturnFeePolicyEntity policy);

    List<ReturnFeePolicyResponse> toPolicyResponses(List<ReturnFeePolicyEntity> policies);
}
