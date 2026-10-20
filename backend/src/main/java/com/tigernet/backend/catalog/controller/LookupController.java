package com.tigernet.backend.catalog.controller;

import com.tigernet.backend.catalog.dto.request.VatOptionRequest;
import com.tigernet.backend.catalog.dto.response.PaymentStatusResponse;
import com.tigernet.backend.catalog.dto.response.VatOptionResponse;
import com.tigernet.backend.catalog.service.LookupService;
import com.tigernet.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/config")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class LookupController {
    LookupService lookupService;

    @GetMapping("/vat-options")
    ApiResponse<List<VatOptionResponse>> getVatOptions() {
        return ApiResponse.success(lookupService.getVatOptions());
    }

    @PostMapping("/vat-options")
    ApiResponse<VatOptionResponse> createVatOption(@RequestBody @Valid VatOptionRequest request) {
        return ApiResponse.success(lookupService.createVatOption(request));
    }

    @PutMapping("/vat-options/{vatId}")
    ApiResponse<VatOptionResponse> updateVatOption(@PathVariable Integer vatId,
                                                   @RequestBody @Valid VatOptionRequest request) {
        return ApiResponse.success(lookupService.updateVatOption(vatId, request));
    }

    @GetMapping("/payment-statuses")
    ApiResponse<List<PaymentStatusResponse>> getPaymentStatuses() {
        return ApiResponse.success(lookupService.getPaymentStatuses());
    }
}
