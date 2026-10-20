package com.tigernet.backend.audit.repository;

import com.tigernet.backend.audit.entity.AuditLogEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;

public interface AuditLogRepository extends JpaRepository<AuditLogEntity, Long> {
    @Query("""
            select a from AuditLogEntity a
            where (:userId is null or a.user.userId = :userId)
              and (:action is null or a.action = :action)
              and (:targetTable is null or a.targetTable = :targetTable)
              and (:targetId is null or a.targetId = :targetId)
              and (cast(:from as LocalDateTime) is null or a.createdAt >= :from)
              and (cast(:to as LocalDateTime) is null or a.createdAt < :to)
            order by a.createdAt desc, a.id desc
            """)
    List<AuditLogEntity> search(Integer userId, String action, String targetTable, Integer targetId,
                                LocalDateTime from, LocalDateTime to);
}
