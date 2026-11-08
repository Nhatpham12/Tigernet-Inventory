package com.tigernet.backend.payment.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.payment.dto.request.PaymentCancelRequest;
import com.tigernet.backend.payment.dto.request.PaymentCreationRequest;
import com.tigernet.backend.payment.dto.response.PaymentResponse;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
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

    @PostMapping("/payments")
    ApiResponse<PaymentResponse> createPayment(@RequestBody @Valid PaymentCreationRequest request) {
        return ApiResponse.success(paymentManagementService.createPayment(request));
    }

    @GetMapping("/payments") // ?direction=&partyId=&referenceType=&referenceId=&from=&to=
    ApiResponse<List<PaymentResponse>> getPayments(
            @RequestParam(required = false) PaymentDirection direction,
            @RequestParam(required = false) Integer partyId,
            @RequestParam(required = false) PaymentReferenceType referenceType,
            @RequestParam(required = false) Integer referenceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return ApiResponse.success(paymentManagementService.getPayments(direction, partyId, referenceType,
                referenceId, from, to));
    }

    @GetMapping("/payments/{paymentId}")
    ApiResponse<PaymentResponse> getPayment(@PathVariable Integer paymentId) {
        return ApiResponse.success(paymentManagementService.getPayment(paymentId));
    }

    @PutMapping("/payments/{paymentId}/cancel")
    ApiResponse<PaymentResponse> cancelPayment(@PathVariable Integer paymentId,
                                               @RequestBody(required = false) PaymentCancelRequest request) {
        return ApiResponse.success(paymentManagementService.cancelPayment(paymentId, request));
    }
}
