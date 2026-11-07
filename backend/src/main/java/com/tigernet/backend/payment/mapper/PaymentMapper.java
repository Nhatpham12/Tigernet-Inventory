package com.tigernet.backend.payment.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.payment.dto.response.PaymentResponse;
import com.tigernet.backend.payment.entity.PaymentEntity;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface PaymentMapper {
    @Mapping(target = "partyId", source = "party.id")
    @Mapping(target = "partyName", source = "party.fullName")
    @Mapping(target = "partyType", source = "party.partyType")
    @Mapping(target = "referenceCode", ignore = true) // service tra theo loại phiếu gốc
    @Mapping(target = "recordedByName", source = "recordedBy.fullName")
    PaymentResponse toResponse(PaymentEntity payment);

    List<PaymentResponse> toResponses(List<PaymentEntity> payments);
}
