package com.tigernet.backend.payment.repository;

import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentDirection;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để 2 người ghi cùng lúc không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentEntity> findFirstByPaymentCodeStartingWithOrderByPaymentCodeDesc(String prefix);

    List<PaymentEntity> findByReferenceTypeAndReferenceIdOrderByPaidAtDescIdDesc(
            PaymentReferenceType referenceType, Integer referenceId);

    // tham số null thì bỏ qua điều kiện đó
    @Query("""
            select p from PaymentEntity p
            where (:direction is null or p.direction = :direction)
              and (:partyId is null or p.party.id = :partyId)
              and (:referenceType is null or p.referenceType = :referenceType)
              and (:referenceId is null or p.referenceId = :referenceId)
              and (cast(:from as LocalDateTime) is null or p.paidAt >= :from)
              and (cast(:to as LocalDateTime) is null or p.paidAt < :to)
            order by p.paidAt desc, p.id desc
            """)
    List<PaymentEntity> search(PaymentDirection direction, Integer partyId, PaymentReferenceType referenceType,
                               Integer referenceId, LocalDateTime from, LocalDateTime to);
}
