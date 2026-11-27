package com.tigernet.backend.stocktake.repository;

import com.tigernet.backend.stocktake.entity.StocktakeEntity;
import com.tigernet.backend.stocktake.enums.StocktakeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StocktakeRepository extends JpaRepository<StocktakeEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<StocktakeEntity> findFirstByStocktakeCodeStartingWithOrderByStocktakeCodeDesc(String prefix);

    // khóa phiếu tới hết transaction: 2 người cùng chốt 1 phiếu thì người sau thấy đã COMPLETED
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from StocktakeEntity s where s.id = :id")
    Optional<StocktakeEntity> findByIdForUpdate(Integer id);

    @Query("""
            select s from StocktakeEntity s
            where (:warehouseId is null or s.warehouse.id = :warehouseId)
              and (:status is null or s.status = :status)
              and (cast(:from as LocalDateTime) is null or s.stocktakeDate >= :from)
              and (cast(:to as LocalDateTime) is null or s.stocktakeDate < :to)
            order by s.stocktakeDate desc, s.id desc
            """)
    List<StocktakeEntity> search(Integer warehouseId, StocktakeStatus status, LocalDateTime from, LocalDateTime to);
}
