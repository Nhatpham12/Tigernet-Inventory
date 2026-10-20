package com.tigernet.backend.inventory.repository;

import com.tigernet.backend.inventory.entity.StockMovementEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface StockMovementRepository extends JpaRepository<StockMovementEntity, Long> {
    @Query("""
            select m from StockMovementEntity m
            where (:warehouseId is null or m.warehouse.id = :warehouseId)
              and (:productId is null or m.product.id = :productId)
              and (cast(:from as LocalDateTime) is null or m.createdAt >= :from)
              and (cast(:to as LocalDateTime) is null or m.createdAt < :to)
            order by m.createdAt desc, m.id desc
            """)
    List<StockMovementEntity> search(Integer warehouseId, Integer productId, LocalDateTime from, LocalDateTime to);
}
