package com.tigernet.backend.supplierreturn.repository;

import com.tigernet.backend.supplierreturn.entity.SupplierReturnEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;

public interface SupplierReturnRepository extends JpaRepository<SupplierReturnEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để 2 người lập phiếu cùng lúc không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SupplierReturnEntity> findFirstByReturnCodeStartingWithOrderByReturnCodeDesc(String prefix);
}
