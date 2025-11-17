package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.AuditAction;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs", indexes = {
    @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id"),
    @Index(name = "idx_audit_user", columnList = "user_id"),
    @Index(name = "idx_audit_timestamp", columnList = "timestamp")
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuditLog {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "entity_type", nullable = false, length = 100)
    private String entityType;  // "GeneralLedger", "ChartOfAccounts", etc.
    
    @Column(name = "entity_id", nullable = false)
    private Long entityId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private AuditAction action;
    
    @Column(name = "user_id")
    private Long userId;
    
    @Column(name = "username", length = 100)
    private String username;
    
    @Column(name = "timestamp", nullable = false)
    @Builder.Default
    private LocalDateTime timestamp = LocalDateTime.now();
    
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;  // JSON
    
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;  // JSON
    
    @Column(name = "ip_address", length = 45)
    private String ipAddress;
    
    @Column(name = "user_agent", length = 255)
    private String userAgent;
    
    @Column(columnDefinition = "TEXT")
    private String changes;  // Description textuelle des changements
}