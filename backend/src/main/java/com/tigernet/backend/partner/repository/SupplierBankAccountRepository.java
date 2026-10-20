package com.tigernet.backend.partner.repository;

import com.tigernet.backend.partner.entity.SupplierBankAccountEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierBankAccountRepository extends JpaRepository<SupplierBankAccountEntity, Integer> {
    List<SupplierBankAccountEntity> findBySupplierId(Integer supplierId);

    Optional<SupplierBankAccountEntity> findByIdAndSupplierId(Integer id, Integer supplierId);
}
