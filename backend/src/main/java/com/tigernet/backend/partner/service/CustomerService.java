package com.tigernet.backend.partner.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.partner.dto.request.CustomerCreationRequest;
import com.tigernet.backend.partner.dto.request.CustomerUpdateRequest;
import com.tigernet.backend.partner.dto.response.CustomerResponse;
import com.tigernet.backend.partner.entity.CustomerEntity;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.mapper.PartnerMapper;
import com.tigernet.backend.partner.repository.CustomerRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class CustomerService {
    CustomerRepository customerRepository;
    PartyService partyService;
    PartnerMapper partnerMapper;

    public CustomerResponse createCustomer(CustomerCreationRequest request) {
        PartyEntity party = partyService.resolveParty(request.getPartyId(), request.getParty());

        // Party_ID UNIQUE: Party này từng là khách (kể cả đã xóa mềm) thì khôi phục dòng cũ
        Optional<CustomerEntity> existing = customerRepository.findByPartyId(party.getId());
        if (existing.isPresent() && !Boolean.TRUE.equals(existing.get().getIsDeleted())) {
            throw new AppException(ErrorCode.CUSTOMER_EXISTED);
        }
        CustomerEntity customer = existing.orElseGet(CustomerEntity::new);
        customer.setParty(party);
        customer.setIsDeleted(false);
        return partnerMapper.toCustomerResponse(customerRepository.save(customer));
    }

    @Transactional(readOnly = true)
    public List<CustomerResponse> getCustomers(String keyword) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return partnerMapper.toCustomerResponses(customerRepository.search(normalized));
    }

    @Transactional(readOnly = true)
    public CustomerResponse getCustomer(Integer customerId) {
        return partnerMapper.toCustomerResponse(findCustomer(customerId));
    }

    public CustomerResponse updateCustomer(Integer customerId, CustomerUpdateRequest request) {
        CustomerEntity customer = findCustomer(customerId);
        partyService.updateParty(customer.getParty(), request.getParty());
        return partnerMapper.toCustomerResponse(customerRepository.save(customer));
    }

    // xóa mềm: phiếu xuất/thuê cũ vẫn tham chiếu được
    public void deleteCustomer(Integer customerId) {
        CustomerEntity customer = findCustomer(customerId);
        customer.setIsDeleted(true);
        customerRepository.save(customer);
    }

    public CustomerEntity findCustomer(Integer customerId) {
        return customerRepository.findByIdAndIsDeletedFalse(customerId)
                .orElseThrow(() -> new AppException(ErrorCode.CUSTOMER_NOT_FOUND));
    }
}
