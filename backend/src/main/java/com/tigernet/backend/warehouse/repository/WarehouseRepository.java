package com.tigernet.backend.warehouse.repository;

import com.tigernet.backend.warehouse.entity.WarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface WarehouseRepository extends JpaRepository<WarehouseEntity, Integer> {
    boolean existsByWarehouseCode(String warehouseCode);

    boolean existsByWarehouseName(String warehouseName);

    boolean existsByWarehouseNameAndIdNot(String warehouseName, Integer id);

    List<WarehouseEntity> findByIsActive(Boolean isActive);
}
