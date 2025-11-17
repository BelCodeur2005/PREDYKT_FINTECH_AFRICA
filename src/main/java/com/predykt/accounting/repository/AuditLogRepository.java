package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.AuditLog;
import com.predykt.accounting.domain.enums.AuditAction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);
    
    Page<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId, Pageable pageable);
    
    List<AuditLog> findByUserId(Long userId);
    
    List<AuditLog> findByAction(AuditAction action);
    
    @Query("SELECT a FROM AuditLog a WHERE a.timestamp BETWEEN :startDate AND :endDate " +
           "ORDER BY a.timestamp DESC")
    List<AuditLog> findByTimestampBetween(@Param("startDate") LocalDateTime startDate,
                                          @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT a FROM AuditLog a WHERE a.entityType = :entityType " +
           "AND a.entityId = :entityId ORDER BY a.timestamp DESC")
    List<AuditLog> findAuditTrail(@Param("entityType") String entityType,
                                   @Param("entityId") Long entityId);
    
    // Nettoyage des anciens logs (RGPD / Rétention)
    void deleteByTimestampBefore(LocalDateTime timestamp);
}