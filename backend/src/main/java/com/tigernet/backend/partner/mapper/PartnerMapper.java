package com.tigernet.backend.partner.mapper;

import com.tigernet.backend.common.config.MapstructConfig;
import com.tigernet.backend.partner.dto.request.PartyRequest;
import com.tigernet.backend.partner.dto.request.PartyUpdateRequest;
import com.tigernet.backend.partner.dto.request.ReceiverRequest;
import com.tigernet.backend.partner.dto.request.SupplierBankAccountRequest;
import com.tigernet.backend.partner.dto.request.SupplierDocumentRequest;
import com.tigernet.backend.partner.dto.response.CustomerResponse;
import com.tigernet.backend.partner.dto.response.PartyResponse;
import com.tigernet.backend.partner.dto.response.ReceiverResponse;
import com.tigernet.backend.partner.dto.response.SupplierBankAccountResponse;
import com.tigernet.backend.partner.dto.response.SupplierDocumentResponse;
import com.tigernet.backend.partner.dto.response.SupplierResponse;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.entity.ReceiverEntity;
import com.tigernet.backend.partner.entity.SupplierBankAccountEntity;
import com.tigernet.backend.partner.entity.SupplierDocumentEntity;
import com.tigernet.backend.partner.entity.SupplierEntity;
import org.mapstruct.BeanMapping;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;
import org.mapstruct.NullValuePropertyMappingStrategy;

import java.util.List;

@Mapper(config = MapstructConfig.class)
public interface PartnerMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    PartyEntity toParty(PartyRequest request);

    PartyResponse toPartyResponse(PartyEntity party);

    List<PartyResponse> toPartyResponses(List<PartyEntity> parties);

    // trường nào request gửi null thì giữ nguyên giá trị cũ
    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateParty(@MappingTarget PartyEntity party, PartyUpdateRequest request);

    SupplierResponse toSupplierResponse(SupplierEntity supplier);

    List<SupplierResponse> toSupplierResponses(List<SupplierEntity> suppliers);

    CustomerResponse toCustomerResponse(CustomerEntity customer);

    List<CustomerResponse> toCustomerResponses(List<CustomerEntity> customers);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    SupplierBankAccountEntity toBankAccount(SupplierBankAccountRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    void updateBankAccount(@MappingTarget SupplierBankAccountEntity bankAccount, SupplierBankAccountRequest request);

    @Mapping(target = "supplierId", source = "supplier.id")
    SupplierBankAccountResponse toBankAccountResponse(SupplierBankAccountEntity bankAccount);

    List<SupplierBankAccountResponse> toBankAccountResponses(List<SupplierBankAccountEntity> bankAccounts);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "supplier", ignore = true)
    @Mapping(target = "uploadedAt", ignore = true)
    SupplierDocumentEntity toDocument(SupplierDocumentRequest request);

    @Mapping(target = "supplierId", source = "supplier.id")
    SupplierDocumentResponse toDocumentResponse(SupplierDocumentEntity document);

    List<SupplierDocumentResponse> toDocumentResponses(List<SupplierDocumentEntity> documents);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    ReceiverEntity toReceiver(ReceiverRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "customer", ignore = true)
    @Mapping(target = "isDeleted", ignore = true)
    void updateReceiver(@MappingTarget ReceiverEntity receiver, ReceiverRequest request);

    @Mapping(target = "customerId", source = "customer.id")
    ReceiverResponse toReceiverResponse(ReceiverEntity receiver);

    List<ReceiverResponse> toReceiverResponses(List<ReceiverEntity> receivers);
}
