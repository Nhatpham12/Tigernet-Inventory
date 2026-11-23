package com.tigernet.backend.catalog.controller;

import com.tigernet.backend.common.security.NoPageCheck;
import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

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

    @RequiresPage({Pages.PRODUCT_GROUPS})
    @PostMapping
    ApiResponse<ProductGroupResponse> createGroup(@RequestBody @Valid ProductGroupRequest request) {
        return ApiResponse.success(productGroupService.createGroup(request));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping
    ApiResponse<List<ProductGroupResponse>> getGroups() {
        return ApiResponse.success(productGroupService.getGroups());
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/{groupId}")
    ApiResponse<ProductGroupResponse> getGroup(@PathVariable Integer groupId) {
        return ApiResponse.success(productGroupService.getGroup(groupId));
    }

    @RequiresPage({Pages.PRODUCT_GROUPS})
    @PutMapping("/{groupId}")
    ApiResponse<ProductGroupResponse> updateGroup(@PathVariable Integer groupId,
                                                  @RequestBody @Valid ProductGroupRequest request) {
        return ApiResponse.success(productGroupService.updateGroup(groupId, request));
    }

    @RequiresPage({Pages.PRODUCT_GROUPS})
    @DeleteMapping("/{groupId}")
    ApiResponse<Void> deleteGroup(@PathVariable Integer groupId) {
        productGroupService.deleteGroup(groupId);
        return ApiResponse.success("Product group has been deleted", null);
    }
}
