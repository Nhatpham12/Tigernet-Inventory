package com.tigernet.backend.stocktake.repository;

import com.tigernet.backend.stocktake.entity.StockAdjustmentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockAdjustmentRepository extends JpaRepository<StockAdjustmentEntity, Integer> {
    @Query("""
            select a from StockAdjustmentEntity a
            where (:stocktakeId is null or a.stocktake.id = :stocktakeId)
              and (:warehouseId is null or a.stocktake.warehouse.id = :warehouseId)
              and (:productId is null or a.product.id = :productId)
            order by a.createdAt desc, a.id desc
            """)
    List<StockAdjustmentEntity> search(Integer stocktakeId, Integer warehouseId, Integer productId);
}
