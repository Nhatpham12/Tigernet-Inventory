package com.tigernet.backend.warranty.repository;

import com.tigernet.backend.warranty.entity.WarrantyClaimEntity;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.List;
import java.util.Optional;

public interface WarrantyClaimRepository extends JpaRepository<WarrantyClaimEntity, Integer> {
    // số phiếu lớn nhất cùng tiền tố, khóa FOR UPDATE để 2 người lập phiếu cùng lúc không trùng số
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<WarrantyClaimEntity> findFirstByClaimCodeStartingWithOrderByClaimCodeDesc(String prefix);

    // lịch sử các lần bảo hành của một đơn vị hàng (theo serial)
    List<WarrantyClaimEntity> findByInventoryIdOrderByReceivedAtDesc(Integer inventoryId);
}
