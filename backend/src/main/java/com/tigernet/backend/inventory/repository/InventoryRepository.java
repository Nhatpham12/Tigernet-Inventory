package com.tigernet.backend.inventory.repository;

import com.tigernet.backend.inventory.entity.InventoryEntity;
import com.tigernet.backend.inventory.enums.InventoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface InventoryRepository extends JpaRepository<InventoryEntity, Integer> {
    boolean existsBySerial(String serial);

    // tham số null thì bỏ qua điều kiện đó
    @Query("""
            select i from InventoryEntity i
            where (:warehouseId is null or i.warehouse.id = :warehouseId)
              and (:productId is null or i.product.id = :productId)
              and (:status is null or i.status = :status)
              and (:serial is null or i.serial like concat('%', :serial, '%'))
            order by i.id desc
            """)
    List<InventoryEntity> search(Integer warehouseId, Integer productId, InventoryStatus status, String serial);
}
