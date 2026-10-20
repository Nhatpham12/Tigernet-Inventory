package com.tigernet.backend.catalog.controller;

import com.tigernet.backend.catalog.dto.request.ProductGroupRequest;
import com.tigernet.backend.catalog.dto.response.ProductGroupResponse;
import com.tigernet.backend.catalog.service.ProductGroupService;
import com.tigernet.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/product-groups")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductGroupController {
    ProductGroupService productGroupService;

    @PostMapping
    ApiResponse<ProductGroupResponse> createGroup(@RequestBody @Valid ProductGroupRequest request) {
        return ApiResponse.success(productGroupService.createGroup(request));
    }

    @GetMapping
    ApiResponse<List<ProductGroupResponse>> getGroups() {
        return ApiResponse.success(productGroupService.getGroups());
    }

    @GetMapping("/{groupId}")
    ApiResponse<ProductGroupResponse> getGroup(@PathVariable Integer groupId) {
        return ApiResponse.success(productGroupService.getGroup(groupId));
    }

    @PutMapping("/{groupId}")
    ApiResponse<ProductGroupResponse> updateGroup(@PathVariable Integer groupId,
                                                  @RequestBody @Valid ProductGroupRequest request) {
        return ApiResponse.success(productGroupService.updateGroup(groupId, request));
    }

    @DeleteMapping("/{groupId}")
    ApiResponse<Void> deleteGroup(@PathVariable Integer groupId) {
        productGroupService.deleteGroup(groupId);
        return ApiResponse.success("Product group has been deleted", null);
    }
}
