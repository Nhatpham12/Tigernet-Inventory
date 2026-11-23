package com.tigernet.backend.catalog.controller;

import com.tigernet.backend.common.security.NoPageCheck;
import com.tigernet.backend.common.security.Pages;
import com.tigernet.backend.common.security.RequiresPage;

import com.tigernet.backend.catalog.dto.request.ProductCreationRequest;
import com.tigernet.backend.catalog.dto.request.ProductUpdateRequest;
import com.tigernet.backend.catalog.dto.response.ProductResponse;
import com.tigernet.backend.catalog.service.ProductService;
import com.tigernet.backend.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/products")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ProductController {
    ProductService productService;

    @RequiresPage({Pages.PRODUCTS_NEW})
    @PostMapping
    ApiResponse<ProductResponse> createProduct(@RequestBody @Valid ProductCreationRequest request) {
        return ApiResponse.success(productService.createProduct(request));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping // ?groupId=&keyword=
    ApiResponse<List<ProductResponse>> getProducts(@RequestParam(required = false) Integer groupId,
                                                   @RequestParam(required = false) String keyword) {
        return ApiResponse.success(productService.getProducts(groupId, keyword));
    }

    @NoPageCheck("Danh mục tra cứu dùng chung cho form, dropdown và dashboard")
    @GetMapping("/{productId}")
    ApiResponse<ProductResponse> getProduct(@PathVariable Integer productId) {
        return ApiResponse.success(productService.getProduct(productId));
    }

    @RequiresPage({Pages.PRODUCTS_NEW})
    @PutMapping("/{productId}")
    ApiResponse<ProductResponse> updateProduct(@PathVariable Integer productId,
                                               @RequestBody @Valid ProductUpdateRequest request) {
        return ApiResponse.success(productService.updateProduct(productId, request));
    }

    @RequiresPage({Pages.PRODUCTS_NEW})
    @DeleteMapping("/{productId}")
    ApiResponse<Void> deleteProduct(@PathVariable Integer productId) {
        productService.deleteProduct(productId);
        return ApiResponse.success("Product has been deleted", null);
    }
}
