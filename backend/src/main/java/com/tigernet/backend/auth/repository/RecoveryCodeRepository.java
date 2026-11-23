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

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from RecoveryCodeEntity r where r.userId = :userId")
    void deleteByUserId(@Param("userId") Integer userId);
}
