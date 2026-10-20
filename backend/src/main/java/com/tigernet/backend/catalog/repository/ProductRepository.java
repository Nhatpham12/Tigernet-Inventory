package com.tigernet.backend.catalog.repository;

import com.tigernet.backend.catalog.entity.ProductEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface ProductRepository extends JpaRepository<ProductEntity, Integer> {
    Optional<ProductEntity> findByIdAndIsDeletedFalse(Integer id);

    // groupId / keyword null thì bỏ qua điều kiện đó
    @Query("""
            select p from ProductEntity p
            where p.isDeleted = false
              and (:groupId is null or p.group.id = :groupId)
              and (:keyword is null or lower(p.productName) like lower(concat('%', :keyword, '%')))
            order by p.productName
            """)
    List<ProductEntity> search(Integer groupId, String keyword);

    boolean existsByProductNameAndGroupId(String productName, Integer groupId);

    boolean existsByProductNameAndGroupIdAndIdNot(String productName, Integer groupId, Integer id);

    boolean existsByGroupIdAndIsDeletedFalse(Integer groupId);
}
