package com.tigernet.backend.partner.repository;

import com.tigernet.backend.partner.entity.CustomerEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface CustomerRepository extends JpaRepository<CustomerEntity, Integer> {
    Optional<CustomerEntity> findByIdAndIsDeletedFalse(Integer id);

    Optional<CustomerEntity> findByPartyId(Integer partyId);

    @Query("""
            select c from CustomerEntity c join fetch c.party p
            where c.isDeleted = false
              and (:keyword is null
                   or lower(p.fullName) like lower(concat('%', :keyword, '%'))
                   or p.taxCode like concat('%', :keyword, '%')
                   or p.nationalId like concat('%', :keyword, '%')
                   or p.phone like concat('%', :keyword, '%'))
            order by p.fullName
            """)
    List<CustomerEntity> search(String keyword);
}
