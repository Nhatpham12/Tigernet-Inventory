package com.tigernet.backend.payment.repository;

import com.tigernet.backend.payment.entity.PaymentEntity;
import com.tigernet.backend.payment.enums.PaymentReferenceType;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface PaymentRepository extends JpaRepository<PaymentEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để 2 người ghi cùng lúc không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<PaymentEntity> findFirstByPaymentCodeStartingWithOrderByPaymentCodeDesc(String prefix);

    List<PaymentEntity> findByReferenceTypeAndReferenceIdOrderByPaidAtDescIdDesc(
            PaymentReferenceType referenceType, Integer referenceId);
}
