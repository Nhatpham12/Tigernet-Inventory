package com.tigernet.backend.catalog.repository;

import com.tigernet.backend.catalog.entity.ProductGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProductGroupRepository extends JpaRepository<ProductGroupEntity, Integer> {
    List<ProductGroupEntity> findByIsDeletedFalseOrderByGroupName();

    Optional<ProductGroupEntity> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByGroupName(String groupName);

    boolean existsByGroupNameAndIdNot(String groupName, Integer id);
}
