package com.tigernet.backend.user.repository;

import com.tigernet.backend.user.entity.UserPagePermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserPagePermissionRepository extends JpaRepository<UserPagePermissionEntity, Integer> {
    List<UserPagePermissionEntity> findByUserIdOrderByPageKey(Integer userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserPagePermissionEntity p where p.userId = :userId")
    void deleteByUserId(Integer userId);
}
