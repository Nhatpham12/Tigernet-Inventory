package com.tigernet.backend.warehouse.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.common.security.SecurityUtils;
import com.tigernet.backend.user.service.PageAccessPolicy;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.repository.UserWarehouseRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Giới hạn dữ liệu theo kho được phân công (User_Warehouses).
 * Quản trị, Quản lý, Kế toán xem mọi kho (giống BFF SessionController.ALL_WAREHOUSE_ROLES);
 * Thủ kho, Bán hàng và vai trò khác chỉ thấy/thao tác phiếu thuộc kho được giao.
 */
@Service
@RequiredArgsConstructor
public class WarehouseAccessService {
    private static final Set<String> ALL_WAREHOUSE_ROLES =
            Set.of(PageAccessPolicy.ADMIN, PageAccessPolicy.MANAGER, PageAccessPolicy.ACCOUNTANT);
    // kho không tồn tại: dùng khi user chưa được giao kho nào để "IN :ids" không rỗng
    private static final List<Integer> NO_WAREHOUSE = List.of(-1);

    private final UserWarehouseRepository userWarehouseRepository;

    /** user đang gọi API xem được mọi kho */
    public boolean all() {
        return ALL_WAREHOUSE_ROLES.contains(SecurityUtils.currentRole());
    }

    /** kho được thao tác; null = mọi kho */
    @Transactional(readOnly = true)
    public Set<Integer> allowedWarehouseIds() {
        if (all()) {
            return null;
        }
        return userWarehouseRepository.findByUserUserId(SecurityUtils.currentUserId()).stream()
                .map(uw -> uw.getWarehouse().getId())
                .collect(Collectors.toUnmodifiableSet());
    }

    /** dùng cho truy vấn "IN :ids": không bao giờ rỗng (chưa được giao kho thì là kho -1) */
    public Collection<Integer> idsOrSentinel() {
        Set<Integer> ids = allowedWarehouseIds();
        return ids == null || ids.isEmpty() ? NO_WAREHOUSE : ids;
    }

    /** ném 403 nếu không được thao tác kho này; null (không chỉ định kho) chỉ hợp lệ với vai trò xem mọi kho */
    public void assertAccess(Integer warehouseId) {
        Set<Integer> ids = allowedWarehouseIds();
        if (ids != null && (warehouseId == null || !ids.contains(warehouseId))) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    public void assertAccess(WarehouseEntity warehouse) {
        assertAccess(warehouse != null ? warehouse.getId() : null);
    }

    /** được thao tác ít nhất một trong các kho (vd phiếu chuyển: kho đi hoặc kho đến) */
    public void assertAnyAccess(WarehouseEntity... warehouses) {
        Set<Integer> ids = allowedWarehouseIds();
        if (ids == null) {
            return;
        }
        boolean ok = Arrays.stream(warehouses).filter(Objects::nonNull).anyMatch(w -> ids.contains(w.getId()));
        if (!ok) {
            throw new AppException(ErrorCode.ACCESS_DENIED);
        }
    }

    /**
     * Lọc danh sách theo kho: có chỉ định [warehouseId] thì kiểm tra quyền kho đó (danh sách đã lọc theo kho);
     * không chỉ định thì bỏ các bản ghi ở kho không được giao. [warehouseOf] trả kho của bản ghi (có thể null).
     */
    public <T> List<T> filter(List<T> rows, Integer warehouseId, Function<T, WarehouseEntity> warehouseOf) {
        Set<Integer> ids = allowedWarehouseIds();
        if (ids == null) {
            return rows;
        }
        if (warehouseId != null) {
            assertAccess(warehouseId);
            return rows;
        }
        return rows.stream().filter(r -> {
            WarehouseEntity w = warehouseOf.apply(r);
            return w != null && ids.contains(w.getId());
        }).toList();
    }
}
