package com.tigernet.backend.rental.repository;

import com.tigernet.backend.rental.entity.ReturnReceiptEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ReturnReceiptRepository extends JpaRepository<ReturnReceiptEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<ReturnReceiptEntity> findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(String prefix);

    @Query("""
            select r from ReturnReceiptEntity r
            where (:warehouseId is null or r.warehouse.id = :warehouseId)
              and (:rentalReceiptId is null or r.rentalReceipt.id = :rentalReceiptId)
            order by r.returnDate desc, r.id desc
            """)
    List<ReturnReceiptEntity> search(Integer warehouseId, Integer rentalReceiptId);

    // tổng số lượng đã trả của 1 dòng thuê qua các phiếu trả trước đó
    @Query("select coalesce(sum(d.quantity), 0) from ReturnReceiptDetailEntity d where d.rentalDetail.id = :rentalDetailId")
    int sumReturnedQuantity(Integer rentalDetailId);
}
