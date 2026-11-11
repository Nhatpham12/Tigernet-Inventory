package com.tigernet.backend.warranty.repository;

import com.tigernet.backend.warranty.entity.WarrantyClaimEntity;
import com.tigernet.backend.warranty.enums.WarrantyClaimStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaimEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để 2 người lập phiếu cùng lúc không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WarrantyClaimEntity> findFirstByClaimCodeStartingWithOrderByClaimCodeDesc(String prefix);

    // lịch sử các lần bảo hành của một đơn vị hàng (theo serial)
    List<WarrantyClaimEntity> findByInventoryIdOrderByReceivedAtDesc(Integer inventoryId);

    @Query("""
            select c from WarrantyClaimEntity c
            where (:status is null or c.status = :status)
              and (:warehouseId is null or c.receivedWarehouse.id = :warehouseId)
              and (:customerId is null or c.exportDetail.exportReceipt.customer.id = :customerId)
              and (cast(:from as LocalDateTime) is null or c.receivedAt >= :from)
              and (cast(:to as LocalDateTime) is null or c.receivedAt < :to)
            order by c.receivedAt desc, c.id desc
            """)
    List<WarrantyClaimEntity> search(WarrantyClaimStatus status, Integer warehouseId, Integer customerId,
                                     LocalDateTime from, LocalDateTime to);
}
