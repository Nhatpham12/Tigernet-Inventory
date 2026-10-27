package com.tigernet.backend.warehouse.repository;

import com.tigernet.backend.warehouse.entity.UserWarehouseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserWarehouseRepository extends JpaRepository<UserWarehouseEntity, Integer> {
    List<UserWarehouseEntity> findByUserUserId(Integer userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserWarehouseEntity uw where uw.user.userId = :userId")
    void deleteByUserId(Integer userId);
}
