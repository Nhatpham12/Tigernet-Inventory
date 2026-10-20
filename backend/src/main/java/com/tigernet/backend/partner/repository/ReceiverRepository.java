package com.tigernet.backend.partner.repository;

import com.tigernet.backend.partner.entity.ReceiverEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ReceiverRepository extends JpaRepository<ReceiverEntity, Integer> {
    List<ReceiverEntity> findByCustomerIdAndIsDeletedFalseOrderByReceiverName(Integer customerId);

    Optional<ReceiverEntity> findByIdAndIsDeletedFalse(Integer id);
}
