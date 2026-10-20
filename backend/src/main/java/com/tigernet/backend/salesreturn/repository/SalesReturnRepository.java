package com.tigernet.backend.salesreturn.repository;

import com.tigernet.backend.salesreturn.entity.SalesReturnEntity;
import com.tigernet.backend.salesreturn.enums.ReturnType;
import com.tigernet.backend.salesreturn.enums.SalesReturnStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SalesReturnRepository extends JpaRepository<SalesReturnEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SalesReturnEntity> findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(String prefix);

    @Query("""
            select s from SalesReturnEntity s
            where (:warehouseId is null or s.warehouse.id = :warehouseId)
              and (:exportReceiptId is null or s.exportReceipt.id = :exportReceiptId)
              and (:returnType is null or s.returnType = :returnType)
              and (:status is null or s.status = :status)
            order by s.returnDate desc, s.id desc
            """)
    List<SalesReturnEntity> search(Integer warehouseId, Integer exportReceiptId,
                                   ReturnType returnType, SalesReturnStatus status);

    // số lượng của 1 dòng bán đã được đổi/trả ở các phiếu khác (không tính phiếu bị từ chối)
    @Query("""
            select coalesce(sum(d.quantity), 0) from SalesReturnDetailEntity d
            where d.exportDetail.id = :exportDetailId
              and d.salesReturn.status <> com.tigernet.backend.salesreturn.enums.SalesReturnStatus.REJECTED
              and (:excludeSalesReturnId is null or d.salesReturn.id <> :excludeSalesReturnId)
            """)
    int sumReturnedQuantity(Integer exportDetailId, Integer excludeSalesReturnId);
}
