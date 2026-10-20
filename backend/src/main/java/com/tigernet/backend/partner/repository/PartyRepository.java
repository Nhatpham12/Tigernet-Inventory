package com.tigernet.backend.partner.repository;

import com.tigernet.backend.partner.entity.PartyEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface PartyRepository extends JpaRepository<PartyEntity, Integer> {
    Optional<PartyEntity> findByIdAndIsDeletedFalse(Integer id);

    boolean existsByTaxCode(String taxCode);

    boolean existsByTaxCodeAndIdNot(String taxCode, Integer id);

    // tìm theo tên, MST, CCCD, SĐT
    @Query("""
            select p from PartyEntity p
            where p.isDeleted = false
              and (:keyword is null
                   or lower(p.fullName) like lower(concat('%', :keyword, '%'))
                   or p.taxCode like concat('%', :keyword, '%')
                   or p.nationalId like concat('%', :keyword, '%')
                   or p.phone like concat('%', :keyword, '%'))
            order by p.fullName
            """)
    List<PartyEntity> search(String keyword);
}
