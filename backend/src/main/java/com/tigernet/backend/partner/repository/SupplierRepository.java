package com.tigernet.backend.partner.repository;

import com.tigernet.backend.partner.entity.SupplierEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface SupplierRepository extends JpaRepository<SupplierEntity, Integer> {
    Optional<SupplierEntity> findByIdAndIsDeletedFalse(Integer id);

    Optional<SupplierEntity> findByPartyId(Integer partyId);

    @Query("""
            select s from SupplierEntity s join fetch s.party p
            where s.isDeleted = false
              and (:keyword is null
                   or lower(p.fullName) like lower(concat('%', :keyword, '%'))
                   or p.taxCode like concat('%', :keyword, '%')
                   or p.phone like concat('%', :keyword, '%'))
            order by p.fullName
            """)
    List<SupplierEntity> search(String keyword);
}
