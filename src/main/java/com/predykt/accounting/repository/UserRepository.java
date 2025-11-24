package com.predykt.accounting.repository;

import com.predykt.accounting.domain.entity.Company;
import com.predykt.accounting.domain.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    
    // Recherche par email
    Optional<User> findByEmail(String email);
    
    // Recherche par email et company (pour multi-tenant)
    Optional<User> findByEmailAndCompany(String email, Company company);
    
    // Recherche par company
    List<User> findByCompany(Company company);
    
    // Utilisateurs actifs d'une company
    List<User> findByCompanyAndIsActiveTrue(Company company);
    
    // Vérifier si email existe
    boolean existsByEmail(String email);
    
    // Vérifier si email existe pour une company
    boolean existsByEmailAndCompany(String email, Company company);
    
    // Recherche par token de vérification email
    Optional<User> findByEmailVerificationToken(String token);
    
    // Recherche par token de réinitialisation mot de passe
    @Query("SELECT u FROM User u WHERE u.passwordResetToken = :token " +
           "AND u.passwordResetExpiresAt > :now")
    Optional<User> findByPasswordResetToken(@Param("token") String token, 
                                            @Param("now") LocalDateTime now);
    
    // Compter les utilisateurs actifs aujourd'hui
    @Query("SELECT COUNT(u) FROM User u WHERE u.lastLoginAt >= :startOfDay")
    long countActiveToday(@Param("startOfDay") LocalDateTime startOfDay);
    
    // Utilisateurs verrouillés
    @Query("SELECT u FROM User u WHERE u.lockedUntil IS NOT NULL " +
           "AND u.lockedUntil > :now")
    List<User> findLockedUsers(@Param("now") LocalDateTime now);
    
    // Utilisateurs non vérifiés depuis X jours
    @Query("SELECT u FROM User u WHERE u.isEmailVerified = false " +
           "AND u.createdAt < :cutoffDate")
    List<User> findUnverifiedUsersBefore(@Param("cutoffDate") LocalDateTime cutoffDate);
}