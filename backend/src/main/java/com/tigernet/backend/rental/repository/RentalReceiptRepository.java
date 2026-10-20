package com.tigernet.backend.rental.repository;

import com.tigernet.backend.rental.entity.RentalReceiptEntity;
import com.tigernet.backend.rental.enums.RentalType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RentalReceiptRepository extends JpaRepository<RentalReceiptEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<RentalReceiptEntity> findFirstByReceiptCodeStartingWithOrderByReceiptCodeDesc(String prefix);

    @Query("""
            select r from RentalReceiptEntity r
            where (:warehouseId is null or r.warehouse.id = :warehouseId)
              and (:customerId is null or r.customer.id = :customerId)
              and (:rentalType is null or r.rentalType = :rentalType)
              and (:paymentStatus is null or r.paymentStatus.paymentStatus = :paymentStatus)
              and (cast(:from as LocalDateTime) is null or r.dateOut >= :from)
              and (cast(:to as LocalDateTime) is null or r.dateOut < :to)
            order by r.dateOut desc, r.id desc
            """)
    List<RentalReceiptEntity> search(Integer warehouseId, Integer customerId, RentalType rentalType,
                                     String paymentStatus, LocalDateTime from, LocalDateTime to);

    // phiếu đang thuê/mượn có hạn trả trước mốc deadline (gồm cả phiếu đã quá hạn)
    @Query("""
            select r from RentalReceiptEntity r
            where r.paymentStatus.paymentStatus = :renting
              and r.dueDate <= :deadline
              and (:warehouseId is null or r.warehouse.id = :warehouseId)
            order by r.dueDate
            """)
    List<RentalReceiptEntity> findDueBefore(String renting, LocalDateTime deadline, Integer warehouseId);
}
