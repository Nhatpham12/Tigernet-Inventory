package com.tigernet.backend.salesreturn.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.salesreturn.dto.request.ReturnFeePolicyRequest;
import com.tigernet.backend.salesreturn.dto.response.ReturnFeePolicyResponse;
import com.tigernet.backend.salesreturn.service.ReturnFeePolicyService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/return-fee-policies")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ReturnFeePolicyController {
    ReturnFeePolicyService policyService;

    @GetMapping
    ApiResponse<List<ReturnFeePolicyResponse>> getPolicies() {
        return ApiResponse.success(policyService.getPolicies());
    }

    @PostMapping
    ApiResponse<ReturnFeePolicyResponse> createPolicy(@RequestBody @Valid ReturnFeePolicyRequest request) {
        return ApiResponse.success(policyService.createPolicy(request));
    }

    @PutMapping("/{policyId}")
    ApiResponse<ReturnFeePolicyResponse> updatePolicy(@PathVariable Integer policyId,
                                                      @RequestBody @Valid ReturnFeePolicyRequest request) {
        return ApiResponse.success(policyService.updatePolicy(policyId, request));
    }
}
