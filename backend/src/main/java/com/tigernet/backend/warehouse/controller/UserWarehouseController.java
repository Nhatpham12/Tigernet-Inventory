package com.tigernet.backend.warehouse.controller;

import com.tigernet.backend.common.response.ApiResponse;
import com.tigernet.backend.warehouse.dto.request.UserWarehouseAssignRequest;
import com.tigernet.backend.warehouse.dto.response.UserWarehouseResponse;
import com.tigernet.backend.warehouse.service.UserWarehouseService;
import jakarta.validation.Valid;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users/{userId}/warehouses")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class UserWarehouseController {
    UserWarehouseService userWarehouseService;

    @GetMapping
    ApiResponse<List<UserWarehouseResponse>> getUserWarehouses(@PathVariable Integer userId) {
        return ApiResponse.success(userWarehouseService.getUserWarehouses(userId));
    }

    @PutMapping
    ApiResponse<List<UserWarehouseResponse>> assignWarehouses(@PathVariable Integer userId,
                                                              @RequestBody @Valid UserWarehouseAssignRequest request) {
        return ApiResponse.success(userWarehouseService.assignWarehouses(userId, request));
    }
}
