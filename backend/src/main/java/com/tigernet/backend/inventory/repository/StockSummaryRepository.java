package com.tigernet.backend.inventory.repository;

import com.tigernet.backend.inventory.entity.StockSummaryEntity;
import com.tigernet.backend.inventory.entity.StockSummaryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface StockSummaryRepository extends JpaRepository<StockSummaryEntity, StockSummaryId> {
    @Query("""
            select s from StockSummaryEntity s
            where (:warehouseId is null or s.id.warehouseId = :warehouseId)
              and (:productId is null or s.id.productId = :productId)
            order by s.id.warehouseId, s.id.productId
            """)
    List<StockSummaryEntity> search(Integer warehouseId, Integer productId);
}
