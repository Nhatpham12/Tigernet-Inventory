package com.tigernet.backend.transfer.repository;

import com.tigernet.backend.transfer.entity.StockTransferEntity;
import com.tigernet.backend.transfer.enums.TransferStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StockTransferRepository extends JpaRepository<StockTransferEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StockTransferEntity> findFirstByTransferCodeStartingWithOrderByTransferCodeDesc(String prefix);

    @Query("""
            select t from StockTransferEntity t
            where (:fromWarehouseId is null or t.fromWarehouse.id = :fromWarehouseId)
              and (:toWarehouseId is null or t.toWarehouse.id = :toWarehouseId)
              and (:status is null or t.status = :status)
            order by t.createdAt desc, t.id desc
            """)
    List<StockTransferEntity> search(Integer fromWarehouseId, Integer toWarehouseId, TransferStatus status);
}
