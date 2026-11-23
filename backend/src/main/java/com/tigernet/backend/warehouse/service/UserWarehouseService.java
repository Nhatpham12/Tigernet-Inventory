package com.tigernet.backend.warehouse.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.security.SecurityConfig;
import com.tigernet.backend.common.security.SecurityUtils;
import com.tigernet.backend.user.entity.UserEntity;
import com.tigernet.backend.user.service.UserService;
import com.tigernet.backend.warehouse.dto.request.UserWarehouseAssignRequest;
import com.tigernet.backend.warehouse.dto.request.UserWarehouseItem;
import com.tigernet.backend.warehouse.dto.response.UserWarehouseResponse;
import com.tigernet.backend.warehouse.entity.UserWarehouseEntity;
import com.tigernet.backend.warehouse.mapper.WarehouseMapper;
import com.tigernet.backend.warehouse.repository.UserWarehouseRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

// phân công nhân viên vào kho
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class UserWarehouseService {
    UserWarehouseRepository userWarehouseRepository;
    WarehouseService warehouseService;
    UserService userService;
    WarehouseMapper warehouseMapper;

    @Transactional(readOnly = true)
    public List<UserWarehouseResponse> getUserWarehouses(Integer userId) {
        // chỉ xem kho của chính mình; Quản trị/Quản lý xem được của người khác.
        // Kiểm tra trước khi tìm user để không lộ userId nào tồn tại.
        if (!userId.equals(SecurityUtils.currentUserId())
                && !SecurityUtils.hasAnyAuthority(SecurityConfig.ADMIN, SecurityConfig.MANAGER)) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
        userService.findUserById(userId);
        return warehouseMapper.toUserWarehouseResponses(userWarehouseRepository.findByUserUserId(userId));
    }

    // thay toàn bộ danh sách kho của user
    public List<UserWarehouseResponse> assignWarehouses(Integer userId, UserWarehouseAssignRequest request) {
        UserEntity user = userService.findUserById(userId);

        long defaultCount = request.getWarehouses().stream()
                .filter(item -> Boolean.TRUE.equals(item.getIsDefault()))
                .count();
        if (defaultCount > 1) {
            throw new AppException(ErrorCode.MULTIPLE_DEFAULT_WAREHOUSES);
        }

        userWarehouseRepository.deleteByUserId(userId);

        Set<Integer> seen = new HashSet<>();
        LocalDateTime now = LocalDateTime.now();
        for (UserWarehouseItem item : request.getWarehouses()) {
            if (!seen.add(item.getWarehouseId())) {
                continue; // bỏ kho bị gửi trùng
            }
            UserWarehouseEntity userWarehouse = new UserWarehouseEntity();
            userWarehouse.setUser(user);
            userWarehouse.setWarehouse(warehouseService.findWarehouse(item.getWarehouseId()));
            userWarehouse.setIsDefault(Boolean.TRUE.equals(item.getIsDefault()));
            userWarehouse.setAssignedAt(now);
            userWarehouseRepository.save(userWarehouse);
        }
        return warehouseMapper.toUserWarehouseResponses(userWarehouseRepository.findByUserUserId(userId));
    }
}
