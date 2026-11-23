package com.tigernet.backend.partner.controller;

import com.tigernet.backend.common.security.NoPageCheck;
import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.partner.dto.request.SupplierBankAccountRequest;
import com.tigernet.backend.partner.dto.request.SupplierCreationRequest;
import com.tigernet.backend.partner.dto.request.SupplierDocumentRequest;
import com.tigernet.backend.partner.dto.request.SupplierUpdateRequest;
import com.tigernet.backend.partner.dto.response.SupplierBankAccountResponse;
import com.tigernet.backend.partner.dto.response.SupplierDocumentResponse;
import com.tigernet.backend.partner.dto.response.SupplierResponse;
import com.tigernet.backend.partner.service.SupplierService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/suppliers")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SupplierController {
    SupplierService supplierService;

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @PostMapping
    ApiResponse<SupplierResponse> createSupplier(@RequestBody @Valid SupplierCreationRequest request) {
        return ApiResponse.success(supplierService.createSupplier(request));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping // ?keyword=
    ApiResponse<List<SupplierResponse>> getSuppliers(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(supplierService.getSuppliers(keyword));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/{supplierId}")
    ApiResponse<SupplierResponse> getSupplier(@PathVariable Integer supplierId) {
        return ApiResponse.success(supplierService.getSupplier(supplierId));
    }

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @PutMapping("/{supplierId}")
    ApiResponse<SupplierResponse> updateSupplier(@PathVariable Integer supplierId,
                                                 @RequestBody @Valid SupplierUpdateRequest request) {
        return ApiResponse.success(supplierService.updateSupplier(supplierId, request));
    }

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @DeleteMapping("/{supplierId}")
    ApiResponse<Void> deleteSupplier(@PathVariable Integer supplierId) {
        supplierService.deleteSupplier(supplierId);
        return ApiResponse.success("Supplier has been deleted", null);
    }

    @RequiresPage({Pages.SUPPLIERS, Pages.IMPORTS, Pages.PAYMENTS_NEW_OUT})
    @GetMapping("/{supplierId}/bank-accounts")
    ApiResponse<List<SupplierBankAccountResponse>> getBankAccounts(@PathVariable Integer supplierId) {
        return ApiResponse.success(supplierService.getBankAccounts(supplierId));
    }

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @PostMapping("/{supplierId}/bank-accounts")
    ApiResponse<SupplierBankAccountResponse> createBankAccount(@PathVariable Integer supplierId,
                                                               @RequestBody @Valid SupplierBankAccountRequest request) {
        return ApiResponse.success(supplierService.createBankAccount(supplierId, request));
    }

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @PutMapping("/{supplierId}/bank-accounts/{bankId}")
    ApiResponse<SupplierBankAccountResponse> updateBankAccount(@PathVariable Integer supplierId,
                                                               @PathVariable Integer bankId,
                                                               @RequestBody @Valid SupplierBankAccountRequest request) {
        return ApiResponse.success(supplierService.updateBankAccount(supplierId, bankId, request));
    }

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @DeleteMapping("/{supplierId}/bank-accounts/{bankId}")
    ApiResponse<Void> deleteBankAccount(@PathVariable Integer supplierId, @PathVariable Integer bankId) {
        supplierService.deleteBankAccount(supplierId, bankId);
        return ApiResponse.success("Bank account has been deleted", null);
    }

    @RequiresPage({Pages.SUPPLIERS, Pages.IMPORTS, Pages.PAYMENTS_NEW_OUT})
    @GetMapping("/{supplierId}/documents")
    ApiResponse<List<SupplierDocumentResponse>> getDocuments(@PathVariable Integer supplierId) {
        return ApiResponse.success(supplierService.getDocuments(supplierId));
    }

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @PostMapping("/{supplierId}/documents")
    ApiResponse<SupplierDocumentResponse> createDocument(@PathVariable Integer supplierId,
                                                         @RequestBody @Valid SupplierDocumentRequest request) {
        return ApiResponse.success(supplierService.createDocument(supplierId, request));
    }

    @RequiresPage({Pages.SUPPLIERS_NEW})
    @DeleteMapping("/{supplierId}/documents/{docId}")
    ApiResponse<Void> deleteDocument(@PathVariable Integer supplierId, @PathVariable Integer docId) {
        supplierService.deleteDocument(supplierId, docId);
        return ApiResponse.success("Document has been deleted", null);
    }
}
