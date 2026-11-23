package com.tigernet.backend.salesreturn.controller;

import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.salesreturn.dto.request.SalesReturnCreationRequest;
import com.tigernet.backend.salesreturn.dto.request.SalesReturnUpdateRequest;
import com.tigernet.backend.salesreturn.dto.response.SalesReturnResponse;
import com.tigernet.backend.salesreturn.enums.ReturnType;
import com.tigernet.backend.salesreturn.enums.SalesReturnStatus;
import com.tigernet.backend.salesreturn.service.SalesReturnService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/sales-returns")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class SalesReturnController {
    SalesReturnService salesReturnService;

    @RequiresPage({Pages.SALES_RETURNS})
    @PostMapping
    ApiResponse<SalesReturnResponse> createSalesReturn(@RequestBody @Valid SalesReturnCreationRequest request) {
        return ApiResponse.success(salesReturnService.createSalesReturn(request));
    }

    @RequiresPage({Pages.SALES_RETURNS, Pages.PAYMENTS})
    @GetMapping // ?warehouseId=&exportReceiptId=&returnType=&status=
    ApiResponse<List<SalesReturnResponse>> getSalesReturns(@RequestParam(required = false) Integer warehouseId,
                                                           @RequestParam(required = false) Integer exportReceiptId,
                                                           @RequestParam(required = false) ReturnType returnType,
                                                           @RequestParam(required = false) SalesReturnStatus status) {
        return ApiResponse.success(salesReturnService.getSalesReturns(warehouseId, exportReceiptId, returnType, status));
    }

    @RequiresPage({Pages.SALES_RETURNS, Pages.PAYMENTS})
    @GetMapping("/{salesReturnId}")
    ApiResponse<SalesReturnResponse> getSalesReturn(@PathVariable Integer salesReturnId) {
        return ApiResponse.success(salesReturnService.getSalesReturn(salesReturnId));
    }

    @RequiresPage({Pages.SALES_RETURNS})
    @PutMapping("/{salesReturnId}")
    ApiResponse<SalesReturnResponse> updateSalesReturn(@PathVariable Integer salesReturnId,
                                                       @RequestBody @Valid SalesReturnUpdateRequest request) {
        return ApiResponse.success(salesReturnService.updateSalesReturn(salesReturnId, request));
    }

    @RequiresPage({Pages.SALES_RETURNS})
    @DeleteMapping("/{salesReturnId}")
    ApiResponse<Void> deleteSalesReturn(@PathVariable Integer salesReturnId) {
        salesReturnService.deleteSalesReturn(salesReturnId);
        return ApiResponse.success("Sales return has been deleted", null);
    }
}
