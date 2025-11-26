package com.predykt.accounting.domain.entity;

import com.predykt.accounting.domain.enums.AccessLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Entité représentant l'accès d'un utilisateur à un dossier client
 * Utilisée uniquement en MODE CABINET
 */
@Entity
@Table(name = "user_company_access", indexes = {
    @Index(name = "idx_user_company_access_user", columnList = "user_id"),
    @Index(name = "idx_user_company_access_company", columnList = "company_id"),
    @Index(name = "idx_user_company_access_compound", columnList = "user_id, company_id, access_level")
})
@IdClass(UserCompanyAccessId.class)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserCompanyAccess {

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @Enumerated(EnumType.STRING)
    @Column(name = "access_level", length = 20, nullable = false)
    @Builder.Default
    private AccessLevel accessLevel = AccessLevel.READ_WRITE;

    @Column(name = "granted_at", nullable = false)
    @Builder.Default
    private LocalDateTime grantedAt = LocalDateTime.now();

    @Column(name = "granted_by", length = 100)
    private String grantedBy;

    /**
     * Vérifie si l'utilisateur a un accès en lecture
     */
    public boolean canRead() {
        return accessLevel != null;
    }

    /**
     * Vérifie si l'utilisateur peut modifier
     */
    public boolean canWrite() {
        return accessLevel == AccessLevel.READ_WRITE || accessLevel == AccessLevel.ADMIN;
    }

    /**
     * Vérifie si l'utilisateur a les droits admin sur le dossier
     */
    public boolean isAdmin() {
        return accessLevel == AccessLevel.ADMIN;
    }
}
