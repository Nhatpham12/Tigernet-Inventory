package com.tigernet.backend.catalog.repository;

import com.tigernet.backend.catalog.entity.PaymentStatusEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PaymentStatusRepository extends JpaRepository<PaymentStatusEntity, Integer> {
    Optional<PaymentStatusEntity> findByPaymentStatus(String paymentStatus);
}
