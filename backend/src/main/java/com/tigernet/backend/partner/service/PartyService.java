package com.tigernet.backend.partner.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.partner.dto.request.PartyRequest;
import com.tigernet.backend.partner.dto.request.PartyUpdateRequest;
import com.tigernet.backend.partner.dto.response.PartyResponse;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.mapper.PartnerMapper;
import com.tigernet.backend.partner.repository.PartyRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class PartyService {
    PartyRepository partyRepository;
    PartnerMapper partnerMapper;

    // dùng khi chọn nguồn nhập (NCC, khách bán lẻ...) hoặc người mang hàng tới đổi/trả
    @Transactional(readOnly = true)
    public List<PartyResponse> searchParties(String keyword) {
        return partnerMapper.toPartyResponses(partyRepository.search(normalize(keyword)));
    }

    @Transactional(readOnly = true)
    public PartyResponse getParty(Integer partyId) {
        return partnerMapper.toPartyResponse(findParty(partyId));
    }

    // NCC/khách hàng tạo mới: dùng Party có sẵn (partyId) hoặc tạo Party mới (party)
    public PartyEntity resolveParty(Integer partyId, PartyRequest request) {
        if (partyId != null) {
            return findParty(partyId);
        }
        if (request == null) {
            throw new AppException(ErrorCode.PARTY_REQUIRED);
        }
        String taxCode = blankToNull(request.getTaxCode());
        if (taxCode != null && partyRepository.existsByTaxCode(taxCode)) {
            throw new AppException(ErrorCode.TAX_CODE_EXISTED);
        }
        PartyEntity party = partnerMapper.toParty(request);
        party.setTaxCode(taxCode); // MST rỗng lưu NULL để không vướng UNIQUE
        return partyRepository.save(party);
    }

    public void updateParty(PartyEntity party, PartyUpdateRequest request) {
        if (request == null) {
            return;
        }
        String taxCode = blankToNull(request.getTaxCode());
        if (taxCode != null && partyRepository.existsByTaxCodeAndIdNot(taxCode, party.getId())) {
            throw new AppException(ErrorCode.TAX_CODE_EXISTED);
        }
        partnerMapper.updateParty(party, request);
        if (request.getTaxCode() != null) {
            party.setTaxCode(taxCode);
        }
    }

    public PartyEntity findParty(Integer partyId) {
        return partyRepository.findByIdAndIsDeletedFalse(partyId)
                .orElseThrow(() -> new AppException(ErrorCode.PARTY_NOT_FOUND));
    }

    private String normalize(String keyword) {
        return StringUtils.hasText(keyword) ? keyword.trim() : null;
    }

    private String blankToNull(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }
}
