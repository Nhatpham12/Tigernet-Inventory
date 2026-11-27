package com.tigernet.backend.inbound.repository;

import com.tigernet.backend.inbound.entity.ImportReceiptEntity;
import com.tigernet.backend.inbound.enums.ImportStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface ImportReceiptRepository extends JpaRepository<ImportReceiptEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để 2 người lập phiếu cùng lúc không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ImportReceiptEntity> findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(String prefix);

    // khóa phiếu tới hết transaction: 2 người cùng xác nhận/sửa/hủy 1 phiếu thì người sau thấy trạng thái mới
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select r from ImportReceiptEntity r where r.id = :id")
    Optional<ImportReceiptEntity> findByIdForUpdate(Integer id);

    @Query("""
            select r from ImportReceiptEntity r
            where (:warehouseId is null or r.warehouse.id = :warehouseId)
              and (:status is null or r.status = :status)
              and (:sourcePartyId is null or r.sourceParty.id = :sourcePartyId)
              and (cast(:from as LocalDateTime) is null or r.dateIn >= :from)
              and (cast(:to as LocalDateTime) is null or r.dateIn < :to)
            order by r.dateIn desc, r.id desc
            """)
    List<ImportReceiptEntity> search(Integer warehouseId, ImportStatus status, Integer sourcePartyId,
                                     LocalDateTime from, LocalDateTime to);
}
