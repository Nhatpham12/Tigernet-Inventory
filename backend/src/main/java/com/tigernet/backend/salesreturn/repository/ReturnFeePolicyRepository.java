package com.tigernet.backend.salesreturn.repository;

import com.tigernet.backend.salesreturn.entity.ReturnFeePolicyEntity;
import com.tigernet.backend.salesreturn.enums.FeeActionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDate;
import java.util.List;

public interface ReturnFeePolicyRepository extends JpaRepository<ReturnFeePolicyEntity, Integer> {
    List<ReturnFeePolicyEntity> findAllByOrderByMaxDaysAscActionTypeAsc();

    // bậc nhỏ nhất còn chứa được số ngày, ưu tiên chính sách hiệu lực gần nhất
    @Query("""
            select p from ReturnFeePolicyEntity p
            where p.actionType = :actionType
              and p.maxDays >= :days
              and (p.effectiveFrom is null or p.effectiveFrom <= :onDate)
            order by p.maxDays asc, p.effectiveFrom desc nulls last
            """)
    List<ReturnFeePolicyEntity> findCandidates(FeeActionType actionType, int days, LocalDate onDate);
}
