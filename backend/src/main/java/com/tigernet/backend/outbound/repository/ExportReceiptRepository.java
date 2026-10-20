package com.tigernet.backend.outbound.repository;

import com.tigernet.backend.outbound.entity.ExportReceiptEntity;
import com.tigernet.backend.outbound.enums.ExportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ExportReceiptRepository extends JpaRepository<ExportReceiptEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ExportReceiptEntity> findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(String prefix);

    @Query("""
            select r from ExportReceiptEntity r
            where (:warehouseId is null or r.warehouse.id = :warehouseId)
              and (:customerId is null or r.customer.id = :customerId)
              and (:status is null or r.status = :status)
              and (cast(:from as LocalDateTime) is null or r.dateOut >= :from)
              and (cast(:to as LocalDateTime) is null or r.dateOut < :to)
            order by r.dateOut desc, r.id desc
            """)
    List<ExportReceiptEntity> search(Integer warehouseId, Integer customerId, ExportStatus status,
                                     LocalDateTime from, LocalDateTime to);
}
