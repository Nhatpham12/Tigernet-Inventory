package com.tigernet.backend.warehouse.repository;

import com.tigernet.backend.warehouse.entity.WarehouseLocationEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseLocationRepository extends JpaRepository<WarehouseLocationEntity, Integer> {
    List<WarehouseLocationEntity> findByWarehouseIdOrderByLocationCode(Integer warehouseId);

    boolean existsByWarehouseIdAndLocationCode(Integer warehouseId, String locationCode);

    boolean existsByWarehouseIdAndLocationCodeAndIdNot(Integer warehouseId, String locationCode, Integer id);
}
