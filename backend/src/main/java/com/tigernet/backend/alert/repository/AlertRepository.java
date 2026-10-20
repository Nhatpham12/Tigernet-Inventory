package com.tigernet.backend.alert.repository;

import com.tigernet.backend.alert.entity.AlertEntity;
import com.tigernet.backend.alert.enums.AlertType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface AlertRepository extends JpaRepository<AlertEntity, Integer> {
    @Query("""
            select a from AlertEntity a
            where (:warehouseId is null or a.warehouse.id = :warehouseId)
              and (:alertType is null or a.alertType = :alertType)
              and (:isResolved is null or a.isResolved = :isResolved)
            order by a.createdAt desc
            """)
    List<AlertEntity> search(Integer warehouseId, AlertType alertType, Boolean isResolved);

    @Query("""
            select count(a) from AlertEntity a
            where a.isResolved = false
              and (:warehouseId is null or a.warehouse.id = :warehouseId)
            """)
    long countUnresolved(Integer warehouseId);
}
