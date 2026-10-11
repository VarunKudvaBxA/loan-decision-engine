package com.loanengine.repo;

import com.loanengine.domain.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    Optional<AuditLog> findTopByOrderByIdDesc();
    List<AuditLog> findAllByOrderByIdAsc();
    List<AuditLog> findByEntityTypeAndEntityIdOrderByIdAsc(String entityType, Long entityId);
}
