package com.tigernet.backend.catalog.mapper;

import com.tigernet.backend.catalog.dto.request.ProductCreationRequest;
import com.tigernet.backend.catalog.dto.request.ProductUpdateRequest;
import com.tigernet.backend.catalog.dto.request.VatOptionRequest;
import com.tigernet.backend.catalog.dto.response.PaymentStatusResponse;
import com.tigernet.backend.catalog.dto.response.ProductGroupResponse;
import com.tigernet.backend.catalog.dto.response.ProductResponse;
import com.tigernet.backend.catalog.dto.response.VatOptionResponse;
import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import com.tigernet.backend.catalog.entity.ProductEntity;
import com.tigernet.backend.catalog.entity.ProductGroupEntity;
import com.tigernet.backend.catalog.entity.VatOptionEntity;
import com.tigernet.backend.common.config.MapstructConfig;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface CatalogMapper {

    ProductGroupResponse toProductGroupResponse(ProductGroupEntity group);

    List<ProductGroupResponse> toProductGroupResponses(List<ProductGroupEntity> groups);

    // group do service tự gán
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    ProductEntity toProduct(ProductCreationRequest request);

    @Mapping(target = "groupId", source = "group.id")
    @Mapping(target = "groupName", source = "group.groupName")
    ProductResponse toProductResponse(ProductEntity product);

    List<ProductResponse> toProductResponses(List<ProductEntity> products);

    // trường nào request gửi null thì giữ nguyên giá trị cũ
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "group", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateProduct(@MappingTarget ProductEntity product, ProductUpdateRequest request);

    @Mapping(target = "id", ignore = true)
    VatOptionEntity toVatOption(VatOptionRequest request);

    VatOptionResponse toVatOptionResponse(VatOptionEntity vatOption);

    List<VatOptionResponse> toVatOptionResponses(List<VatOptionEntity> vatOptions);

    List<PaymentStatusResponse> toPaymentStatusResponses(List<PaymentStatusEntity> paymentStatuses);
}
