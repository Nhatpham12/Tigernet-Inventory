package com.tigernet.backend.partner.controller;

import com.tigernet.backend.common.security.NoPageCheck;
import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.partner.dto.request.CustomerCreationRequest;
import com.tigernet.backend.partner.dto.request.CustomerUpdateRequest;
import com.tigernet.backend.partner.dto.request.ReceiverRequest;
import com.tigernet.backend.partner.dto.response.CustomerResponse;
import com.tigernet.backend.partner.dto.response.ReceiverResponse;
import com.tigernet.backend.partner.service.CustomerService;
import com.tigernet.backend.partner.service.ReceiverService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class CustomerController {
    CustomerService customerService;
    ReceiverService receiverService;

    @RequiresPage({Pages.CUSTOMERS})
    @PostMapping("/customers")
    ApiResponse<CustomerResponse> createCustomer(@RequestBody @Valid CustomerCreationRequest request) {
        return ApiResponse.success(customerService.createCustomer(request));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/customers") // ?keyword=
    ApiResponse<List<CustomerResponse>> getCustomers(@RequestParam(required = false) String keyword) {
        return ApiResponse.success(customerService.getCustomers(keyword));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/customers/{customerId}")
    ApiResponse<CustomerResponse> getCustomer(@PathVariable Integer customerId) {
        return ApiResponse.success(customerService.getCustomer(customerId));
    }

    @RequiresPage({Pages.CUSTOMERS})
    @PutMapping("/customers/{customerId}")
    ApiResponse<CustomerResponse> updateCustomer(@PathVariable Integer customerId,
                                                 @RequestBody @Valid CustomerUpdateRequest request) {
        return ApiResponse.success(customerService.updateCustomer(customerId, request));
    }

    @RequiresPage({Pages.CUSTOMERS})
    @DeleteMapping("/customers/{customerId}")
    ApiResponse<Void> deleteCustomer(@PathVariable Integer customerId) {
        customerService.deleteCustomer(customerId);
        return ApiResponse.success("Customer has been deleted", null);
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/customers/{customerId}/receivers")
    ApiResponse<List<ReceiverResponse>> getReceivers(@PathVariable Integer customerId) {
        return ApiResponse.success(receiverService.getReceivers(customerId));
    }

    @RequiresPage({Pages.CUSTOMERS, Pages.EXPORTS_NEW, Pages.RENTALS_NEW, Pages.LENDS_NEW})
    @PostMapping("/customers/{customerId}/receivers")
    ApiResponse<ReceiverResponse> createReceiver(@PathVariable Integer customerId,
                                                 @RequestBody @Valid ReceiverRequest request) {
        return ApiResponse.success(receiverService.createReceiver(customerId, request));
    }

    @RequiresPage({Pages.CUSTOMERS, Pages.EXPORTS_NEW, Pages.RENTALS_NEW, Pages.LENDS_NEW})
    @PutMapping("/receivers/{receiverId}")
    ApiResponse<ReceiverResponse> updateReceiver(@PathVariable Integer receiverId,
                                                 @RequestBody @Valid ReceiverRequest request) {
        return ApiResponse.success(receiverService.updateReceiver(receiverId, request));
    }

    @RequiresPage({Pages.CUSTOMERS, Pages.EXPORTS_NEW, Pages.RENTALS_NEW, Pages.LENDS_NEW})
    @DeleteMapping("/receivers/{receiverId}")
    ApiResponse<Void> deleteReceiver(@PathVariable Integer receiverId) {
        receiverService.deleteReceiver(receiverId);
        return ApiResponse.success("Receiver has been deleted", null);
    }
}
