package com.tigernet.backend.warehouse.repository;

import com.tigernet.backend.warehouse.entity.UserWarehouseEntity;
import com.tigernet.backend.warehouse.entity.UserWarehouseId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface UserWarehouseRepository extends JpaRepository<UserWarehouseEntity, UserWarehouseId> {
    List<UserWarehouseEntity> findByIdUserId(Integer userId);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("delete from UserWarehouseEntity uw where uw.id.userId = :userId")
    void deleteByUserId(Integer userId);
}
