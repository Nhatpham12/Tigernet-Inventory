package com.tigernet.backend.partner.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.partner.dto.request.SupplierBankAccountRequest;
import com.tigernet.backend.partner.dto.request.SupplierCreationRequest;
import com.tigernet.backend.partner.dto.request.SupplierDocumentRequest;
import com.tigernet.backend.partner.dto.request.SupplierUpdateRequest;
import com.tigernet.backend.partner.dto.response.SupplierBankAccountResponse;
import com.tigernet.backend.partner.dto.response.SupplierDocumentResponse;
import com.tigernet.backend.partner.dto.response.SupplierResponse;
import com.tigernet.backend.partner.entity.PartyEntity;
import com.tigernet.backend.partner.entity.SupplierBankAccountEntity;
import com.tigernet.backend.partner.entity.SupplierDocumentEntity;
import com.tigernet.backend.partner.entity.SupplierEntity;
import com.tigernet.backend.partner.mapper.PartnerMapper;
import com.tigernet.backend.partner.repository.SupplierBankAccountRepository;
import com.tigernet.backend.partner.repository.SupplierDocumentRepository;
import com.tigernet.backend.partner.repository.SupplierRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class SupplierService {
    SupplierRepository supplierRepository;
    SupplierBankAccountRepository bankAccountRepository;
    SupplierDocumentRepository documentRepository;
    PartyService partyService;
    PartnerMapper partnerMapper;

    public SupplierResponse createSupplier(SupplierCreationRequest request) {
        PartyEntity party = partyService.resolveParty(request.getPartyId(), request.getParty());

        // Party_ID UNIQUE: Party này từng là NCC (kể cả đã xóa mềm) thì khôi phục dòng cũ
        Optional<SupplierEntity> existing = supplierRepository.findByPartyId(party.getId());
        if (existing.isPresent() && !Boolean.TRUE.equals(existing.get().getIsDeleted())) {
            throw new AppException(ErrorCode.SUPPLIER_EXISTED);
        }
        SupplierEntity supplier = existing.orElseGet(SupplierEntity::new);
        supplier.setParty(party);
        supplier.setContractNo(request.getContractNo());
        supplier.setTrackingCode(request.getTrackingCode());
        supplier.setIsDeleted(false);
        return partnerMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    @Transactional(readOnly = true)
    public List<SupplierResponse> getSuppliers(String keyword) {
        String normalized = StringUtils.hasText(keyword) ? keyword.trim() : null;
        return partnerMapper.toSupplierResponses(supplierRepository.search(normalized));
    }

    @Transactional(readOnly = true)
    public SupplierResponse getSupplier(Integer supplierId) {
        return partnerMapper.toSupplierResponse(findSupplier(supplierId));
    }

    public SupplierResponse updateSupplier(Integer supplierId, SupplierUpdateRequest request) {
        SupplierEntity supplier = findSupplier(supplierId);
        partyService.updateParty(supplier.getParty(), request.getParty());
        if (request.getContractNo() != null) supplier.setContractNo(request.getContractNo());
        if (request.getTrackingCode() != null) supplier.setTrackingCode(request.getTrackingCode());
        return partnerMapper.toSupplierResponse(supplierRepository.save(supplier));
    }

    // xóa mềm vai trò NCC; Party vẫn giữ vì có thể đang là khách hàng / được phiếu nhập tham chiếu
    public void deleteSupplier(Integer supplierId) {
        SupplierEntity supplier = findSupplier(supplierId);
        supplier.setIsDeleted(true);
        supplierRepository.save(supplier);
    }

    // ---------- tài khoản ngân hàng ----------

    @Transactional(readOnly = true)
    public List<SupplierBankAccountResponse> getBankAccounts(Integer supplierId) {
        findSupplier(supplierId);
        return partnerMapper.toBankAccountResponses(bankAccountRepository.findBySupplierId(supplierId));
    }

    public SupplierBankAccountResponse createBankAccount(Integer supplierId, SupplierBankAccountRequest request) {
        SupplierBankAccountEntity bankAccount = partnerMapper.toBankAccount(request);
        bankAccount.setSupplier(findSupplier(supplierId));
        return partnerMapper.toBankAccountResponse(bankAccountRepository.save(bankAccount));
    }

    public SupplierBankAccountResponse updateBankAccount(Integer supplierId, Integer bankId,
                                                         SupplierBankAccountRequest request) {
        SupplierBankAccountEntity bankAccount = findBankAccount(supplierId, bankId);
        partnerMapper.updateBankAccount(bankAccount, request);
        return partnerMapper.toBankAccountResponse(bankAccountRepository.save(bankAccount));
    }

    public void deleteBankAccount(Integer supplierId, Integer bankId) {
        bankAccountRepository.delete(findBankAccount(supplierId, bankId));
    }

    // ---------- chứng từ ----------

    @Transactional(readOnly = true)
    public List<SupplierDocumentResponse> getDocuments(Integer supplierId) {
        findSupplier(supplierId);
        return partnerMapper.toDocumentResponses(documentRepository.findBySupplierIdOrderByUploadedAtDesc(supplierId));
    }

    public SupplierDocumentResponse createDocument(Integer supplierId, SupplierDocumentRequest request) {
        SupplierDocumentEntity document = partnerMapper.toDocument(request);
        document.setSupplier(findSupplier(supplierId));
        document.setUploadedAt(LocalDateTime.now());
        return partnerMapper.toDocumentResponse(documentRepository.save(document));
    }

    // chỉ xóa bản ghi; xóa file trên object storage do module storage xử lý
    public void deleteDocument(Integer supplierId, Integer docId) {
        SupplierDocumentEntity document = documentRepository.findByIdAndSupplierId(docId, supplierId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_DOCUMENT_NOT_FOUND));
        documentRepository.delete(document);
    }

    public SupplierEntity findSupplier(Integer supplierId) {
        return supplierRepository.findByIdAndIsDeletedFalse(supplierId)
                .orElseThrow(() -> new AppException(ErrorCode.SUPPLIER_NOT_FOUND));
    }

    private SupplierBankAccountEntity findBankAccount(Integer supplierId, Integer bankId) {
        findSupplier(supplierId);
        return bankAccountRepository.findByIdAndSupplierId(bankId, supplierId)
                .orElseThrow(() -> new AppException(ErrorCode.BANK_ACCOUNT_NOT_FOUND));
    }
}
