package com.tigernet.backend.warranty.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.warranty.dto.request.WarrantyClaimCreationRequest;
import com.tigernet.backend.warranty.dto.request.WarrantyStatusRequest;
import com.tigernet.backend.warranty.dto.response.WarrantyClaimResponse;
import com.tigernet.backend.warranty.dto.response.WarrantyLookupResponse;
import com.tigernet.backend.warranty.enums.WarrantyClaimStatus;
import com.tigernet.backend.warranty.service.WarrantyClaimService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class WarrantyClaimController {
    WarrantyClaimService warrantyClaimService;

    // ?serial= (dòng bán gần nhất của serial) hoặc ?exportReceiptId= (mọi dòng của phiếu xuất)
    @GetMapping("/warranty-claims/lookup")
    ApiResponse<List<WarrantyLookupResponse>> lookup(@RequestParam(required = false) String serial,
                                                     @RequestParam(required = false) Integer exportReceiptId) {
        return ApiResponse.success(warrantyClaimService.lookup(serial, exportReceiptId));
    }

    @PostMapping("/warranty-claims")
    ApiResponse<WarrantyClaimResponse> createClaim(@RequestBody @Valid WarrantyClaimCreationRequest request) {
        return ApiResponse.success(warrantyClaimService.createClaim(request));
    }

    @GetMapping("/warranty-claims") // ?status=&warehouseId=&customerId=&from=&to=
    ApiResponse<List<WarrantyClaimResponse>> getClaims(
            @RequestParam(required = false) WarrantyClaimStatus status,
            @RequestParam(required = false) Integer warehouseId,
            @RequestParam(required = false) Integer customerId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(warrantyClaimService.getClaims(status, warehouseId, customerId, from, to));
    }

    @GetMapping("/warranty-claims/{claimId}")
    ApiResponse<WarrantyClaimResponse> getClaim(@PathVariable Integer claimId) {
        return ApiResponse.success(warrantyClaimService.getClaim(claimId));
    }

    @PutMapping("/warranty-claims/{claimId}/status")
    ApiResponse<WarrantyClaimResponse> changeStatus(@PathVariable Integer claimId,
                                                    @RequestBody @Valid WarrantyStatusRequest request) {
        return ApiResponse.success(warrantyClaimService.changeStatus(claimId, request));
    }
}
