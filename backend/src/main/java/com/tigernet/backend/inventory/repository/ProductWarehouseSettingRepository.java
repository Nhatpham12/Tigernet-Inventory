package com.tigernet.backend.inventory.repository;

import com.tigernet.backend.inventory.entity.ProductWarehouseSettingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductWarehouseSettingRepository extends JpaRepository<ProductWarehouseSettingEntity, Integer> {
    Optional<ProductWarehouseSettingEntity> findByProductIdAndWarehouseId(Integer productId, Integer warehouseId);

    @Query("""
            select s from ProductWarehouseSettingEntity s
            where (:warehouseId is null or s.warehouse.id = :warehouseId)
              and (:productId is null or s.product.id = :productId)
            order by s.warehouse.id, s.product.id
            """)
    List<ProductWarehouseSettingEntity> search(Integer warehouseId, Integer productId);
}
