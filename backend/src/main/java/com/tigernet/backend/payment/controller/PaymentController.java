package com.tigernet.backend.payment.controller;

import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.payment.dto.request.PaymentCancelRequest;
import com.tigernet.backend.payment.dto.request.PaymentCreationRequest;
import com.tigernet.backend.payment.dto.response.PaymentResponse;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import com.tigernet.backend.payment.service.PaymentAccess;
import com.tigernet.backend.payment.service.PaymentManagementService;
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
public class PaymentController {
    PaymentManagementService paymentManagementService;
    PaymentAccess paymentAccess;

    @RequiresPage({Pages.PAYMENTS_NEW_IN, Pages.PAYMENTS_NEW_OUT})
    @PostMapping("/payments")
    ApiResponse<PaymentResponse> createPayment(@RequestBody @Valid PaymentCreationRequest request) {
        paymentAccess.requireWrite(request.getDirection());
        return ApiResponse.success(paymentManagementService.createPayment(request));
    }

    @RequiresPage({Pages.PAYMENTS, Pages.IMPORTS, Pages.EXPORTS})
    @GetMapping("/payments") // ?direction=&partyId=&referenceType=&referenceId=&from=&to=
    ApiResponse<List<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentDirection direction,
            @RequestParam(required = false) Integer partyId,
            @RequestParam(required = false) PaymentReferenceType referenceType,
            @RequestParam(required = false) Integer referenceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(paymentManagementService.getPayments(paymentAccess.listDirection(direction),
                partyId, referenceType, referenceId, from, to));
    }

    @RequiresPage({Pages.PAYMENTS, Pages.IMPORTS, Pages.EXPORTS})
    @GetMapping("/payments/{paymentId}")
    ApiResponse<PaymentResponse> getPayment(@PathVariable Integer paymentId) {
        PaymentResponse payment = paymentManagementService.getPayment(paymentId);
        paymentAccess.requireView(payment.getDirection());
        return ApiResponse.success(payment);
    }

    @RequiresPage({Pages.PAYMENTS_NEW_IN, Pages.PAYMENTS_NEW_OUT})
    @PutMapping("/payments/{paymentId}/cancel")
    ApiResponse<PaymentResponse> cancelPayment(@PathVariable Integer paymentId,
                                               @RequestBody(required = false) PaymentCancelRequest request) {
        paymentAccess.requireWrite(paymentManagementService.getPayment(paymentId).getDirection());
        return ApiResponse.success(paymentManagementService.cancelPayment(paymentId, request));
    }
}
