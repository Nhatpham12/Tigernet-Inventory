package com.tigernet.backend.partner.repository;

import com.tigernet.backend.partner.entity.SupplierDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SupplierDocumentRepository extends JpaRepository<SupplierDocumentEntity, Integer> {
    List<SupplierDocumentEntity> findBySupplierIdOrderByUploadedAtDesc(Integer supplierId);

    Optional<SupplierDocumentEntity> findByIdAndSupplierId(Integer id, Integer supplierId);
}
