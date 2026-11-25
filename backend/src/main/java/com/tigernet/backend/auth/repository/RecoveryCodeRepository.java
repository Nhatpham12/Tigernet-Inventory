package com.tigernet.backend.auth.repository;

import com.tigernet.backend.auth.entity.RecoveryCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RecoveryCodeRepository extends JpaRepository<RecoveryCodeEntity, Integer> {
    List<RecoveryCodeEntity> findByUserId(Integer userId);

    // KHÔNG clearAutomatically: hàm được gọi giữa transaction (bật MFA rồi cấp token ngay); clear sẽ tách
    // UserEntity đang dùng, đọc role (lazy) sau đó ném LazyInitializationException → HTTP 500.
    // Không cần clear vì RecoveryCodeEntity không được nạp vào context trước khi xoá trong cùng transaction.
    @Modifying(flushAutomatically = true)
    @Query("delete from RecoveryCodeEntity r where r.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
