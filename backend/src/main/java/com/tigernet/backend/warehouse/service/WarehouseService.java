package com.tigernet.backend.warehouse.service;

import com.tigernet.backend.common.exception.AppException;
import com.tigernet.backend.common.exception.ErrorCode;
import com.tigernet.backend.user.service.UserService;
import com.tigernet.backend.warehouse.dto.request.WarehouseCreationRequest;
import com.tigernet.backend.warehouse.dto.request.WarehouseLocationRequest;
import com.tigernet.backend.warehouse.dto.request.WarehouseUpdateRequest;
import com.tigernet.backend.warehouse.dto.response.WarehouseLocationResponse;
import com.tigernet.backend.warehouse.dto.response.WarehouseResponse;
import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import com.tigernet.backend.warehouse.entity.WarehouseLocationEntity;
import com.tigernet.backend.warehouse.mapper.WarehouseMapper;
import com.tigernet.backend.warehouse.repository.WarehouseLocationRepository;
import com.tigernet.backend.warehouse.repository.WarehouseRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@Transactional
public class WarehouseService {
    WarehouseRepository warehouseRepository;
    WarehouseLocationRepository locationRepository;
    WarehouseMapper warehouseMapper;
    UserService userService;

    public WarehouseResponse createWarehouse(WarehouseCreationRequest request) {
        if (warehouseRepository.existsByWarehouseCode(request.getWarehouseCode())) {
            throw new AppException(ErrorCode.WAREHOUSE_CODE_EXISTED);
        }
        if (warehouseRepository.existsByWarehouseName(request.getWarehouseName())) {
            throw new AppException(ErrorCode.WAREHOUSE_NAME_EXISTED);
        }

        WarehouseEntity warehouse = warehouseMapper.toWarehouse(request);
        warehouse.setCreatedAt(LocalDateTime.now());
        if (warehouse.getIsActive() == null) {
            warehouse.setIsActive(true);
        }
        if (request.getManagerId() != null) {
            warehouse.setManager(userService.findUserById(request.getManagerId()));
        }
        return warehouseMapper.toWarehouseResponse(warehouseRepository.save(warehouse));
    }

    @Transactional(readOnly = true)
    public List<WarehouseResponse> getWarehouses(Boolean isActive) {
        List<WarehouseEntity> warehouses = isActive == null
                ? warehouseRepository.findAll()
                : warehouseRepository.findByIsActive(isActive);
        return warehouseMapper.toWarehouseResponses(warehouses);
    }

    @Transactional(readOnly = true)
    public WarehouseResponse getWarehouse(Integer warehouseId) {
        return warehouseMapper.toWarehouseResponse(findWarehouse(warehouseId));
    }

    public WarehouseResponse updateWarehouse(Integer warehouseId, WarehouseUpdateRequest request) {
        WarehouseEntity warehouse = findWarehouse(warehouseId);
        if (request.getWarehouseName() != null
                && warehouseRepository.existsByWarehouseNameAndIdNot(request.getWarehouseName(), warehouseId)) {
            throw new AppException(ErrorCode.WAREHOUSE_NAME_EXISTED);
        }
        warehouseMapper.updateWarehouse(warehouse, request);
        if (request.getManagerId() != null) {
            warehouse.setManager(userService.findUserById(request.getManagerId()));
        }
        return warehouseMapper.toWarehouseResponse(warehouseRepository.save(warehouse));
    }

    // kho đã có phiếu/tồn thì DB chặn (FK RESTRICT) -> báo lỗi, nên ngừng dùng bằng isActive=false
    public void deleteWarehouse(Integer warehouseId) {
        WarehouseEntity warehouse = findWarehouse(warehouseId);
        try {
            warehouseRepository.delete(warehouse);
            warehouseRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new AppException(ErrorCode.WAREHOUSE_IN_USE);
        }
    }

    // ---------- vị trí trong kho ----------

    @Transactional(readOnly = true)
    public List<WarehouseLocationResponse> getLocations(Integer warehouseId) {
        findWarehouse(warehouseId);
        return warehouseMapper.toLocationResponses(locationRepository.findByWarehouseIdOrderByLocationCode(warehouseId));
    }

    public WarehouseLocationResponse createLocation(Integer warehouseId, WarehouseLocationRequest request) {
        WarehouseEntity warehouse = findWarehouse(warehouseId);
        if (locationRepository.existsByWarehouseIdAndLocationCode(warehouseId, request.getLocationCode())) {
            throw new AppException(ErrorCode.LOCATION_CODE_EXISTED);
        }
        WarehouseLocationEntity location = warehouseMapper.toLocation(request);
        location.setWarehouse(warehouse);
        return warehouseMapper.toLocationResponse(locationRepository.save(location));
    }

    public WarehouseLocationResponse updateLocation(Integer locationId, WarehouseLocationRequest request) {
        WarehouseLocationEntity location = findLocation(locationId);
        if (locationRepository.existsByWarehouseIdAndLocationCodeAndIdNot(
                location.getWarehouse().getId(), request.getLocationCode(), locationId)) {
            throw new AppException(ErrorCode.LOCATION_CODE_EXISTED);
        }
        warehouseMapper.updateLocation(location, request);
        return warehouseMapper.toLocationResponse(locationRepository.save(location));
    }

    // Inventory.Location_ID là ON DELETE SET NULL nên xóa được
    public void deleteLocation(Integer locationId) {
        locationRepository.delete(findLocation(locationId));
    }

    // ---------- dùng cho module khác ----------

    public WarehouseEntity findWarehouse(Integer warehouseId) {
        return warehouseRepository.findById(warehouseId)
                .orElseThrow(() -> new AppException(ErrorCode.WAREHOUSE_NOT_FOUND));
    }

    // dùng khi lập phiếu mới: kho phải đang hoạt động
    public WarehouseEntity findActiveWarehouse(Integer warehouseId) {
        WarehouseEntity warehouse = findWarehouse(warehouseId);
        if (!Boolean.TRUE.equals(warehouse.getIsActive())) {
            throw new AppException(ErrorCode.WAREHOUSE_INACTIVE);
        }
        return warehouse;
    }

    public WarehouseLocationEntity findLocation(Integer locationId) {
        return locationRepository.findById(locationId)
                .orElseThrow(() -> new AppException(ErrorCode.LOCATION_NOT_FOUND));
    }
}
